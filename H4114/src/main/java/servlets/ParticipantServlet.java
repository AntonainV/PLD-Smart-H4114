/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.insalyon.videostream.ServerEndPoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Participant;
import model.Survey;
import service.DBConnection;

/**
 *
 * @author Arthur
 */
@WebServlet(name = "ParticipantServlet", urlPatterns = {"/ParticipantServlet"})
public class ParticipantServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String action = request.getParameter("action");
        Connection conn = null;
        PrintWriter out;
        out = response.getWriter();
        Survey survey;
        switch (action) {
            case "room" :
            {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject jsonResponse = new JsonObject();
                HashMap<String, Boolean> rooms = ServerEndPoint.getServerEndPointState();
                HashMap<String, Set <ServerEndPoint>> persons = ServerEndPoint.getServerEndPoints();
                HashMap<String, ServerEndPoint> starters = ServerEndPoint.getServerEndPointStart();
                JsonArray jsonListe = new JsonArray();
                if(!rooms.entrySet().isEmpty()){
                    for (Map.Entry<String, Boolean> entry : rooms.entrySet()) {
                        if(Objects.equals(entry.getValue(), Boolean.FALSE)){
                            JsonObject json = new JsonObject();
                            json.addProperty("num", entry.getKey());
                            json.addProperty("person", persons.get(entry.getKey()).size());
                            json.addProperty("starter", starters.get(entry.getKey()).getName());
                            jsonListe.add(json);
                        }
                    }
                }
                jsonResponse.add("rooms", jsonListe);
                out.println(gson.toJson(jsonResponse));
                out.close();
                break;
            }
            case "create" :
            {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject jsonResponse = new JsonObject();
                int index = ServerEndPoint.getNumber();
                jsonResponse.addProperty("index", index);
                out.println(gson.toJson(jsonResponse));
                out.close();
                break;
            }
            case "voteSurvey":
            {
                try {
                    conn = DBConnection.Connection();
                    Participant participant = (Participant) request.getSession().getAttribute("participant");
                    if (participant == null) {
                        break;
                    }

                    survey = Survey.getSurvey(participant.getAssembly().getId());

                    if (survey == null) {
                        break;
                    }
                    System.out.println("sdddddddddddfsdfsdfsdfsdfsdf");
                  
                   
                    String reponse = request.getParameter("response");
                    String address = survey.addAnswerVote(reponse, participant);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject vote = new JsonObject();
                    if (address.isEmpty())
                    {
                        vote.addProperty("alreadyVote", true);
                    }
                    else
                    {
                        vote.addProperty("alreadyVote", false);
                    }
                    
                    out.println(gson.toJson(vote));

                    //request.getSession().setAttribute("addressVote", address);

                } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }
            case "startSurvey":
            {
                Participant participant = (Participant) request.getSession().getAttribute("participant");
                if (participant == null || participant.getStatus() < 2) {
                    break;
                }
                survey = Survey.getSurvey(participant.getAssembly().getId());

                survey.start();

                break;
            }  
            case "removeParticipate":
            {
                Participant participant = (Participant) request.getSession().getAttribute("participant");
                if (participant == null) {
                    break;
                }
                
                
                
                try {
                    conn = DBConnection.Connection();
                    Participant.Remove(conn, participant.getId());
                } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(ParticipantServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                 request.getSession().removeAttribute("participant");
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject surveyInfo = new JsonObject();
                surveyInfo.addProperty("removeParticipate", "true");
                out.println(gson.toJson(surveyInfo)); 
               

                break;
            }  
            case "getResultSurvey":
            {
                JsonObject responseR = new JsonObject();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                
                Participant participant = (Participant) request.getSession().getAttribute("participant");
                if (participant == null ) {
                    break;
                }
                survey = Survey.getSurvey(participant.getAssembly().getId());
                
                responseR.add("Survey", survey.toJson());
                responseR.addProperty("canCreate", participant.getStatus()>=2 &&
                        survey.getStat() == 2);
                
                out.println(gson.toJson(responseR));
               
                break;
            }
            case "createSurvey":
            {
                try {
                    conn = DBConnection.Connection();
                    Participant participant = (Participant) request.getSession().getAttribute("participant");
                    /*if (participant == null || !participant.getStatus().contains("2")) {
                        break;
                    }

                    survey = participant.getAssembly().getCurrentSurvey();

                    if (survey != null) {
                        break;
                    }
                    */    
                    String question = request.getParameter("question");
                    String choicesS = request.getParameter("choices");
                    String time = request.getParameter("time");

                    survey = new Survey(question, time);

                    String[] choices = choicesS.split(";");
                    for (int i = 0; i < choices.length; i++) {
                        survey.addResponseChoice(choices[i]);
                    }
                    
                    Survey.addSurvey(participant.getAssembly().getId(), survey);
                    JsonObject surveyInfo = new JsonObject();
                    
                    
                    

                  if (Survey.Insert(conn, survey)) {
                      Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject cSurvey = new JsonObject();

                      cSurvey.addProperty("createdSurvey", "true");
                        System.out.println(survey);
                        System.out.println(survey.getId());
                        cSurvey.addProperty("id_survey", survey.getId().toString());
                        surveyInfo.add("survey", cSurvey);
                        out.println(gson.toJson(surveyInfo));

                        request.getSession().setAttribute("survey", survey); 
                        survey.start();
                        
                        
                    } else {
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        JsonObject cSurvey = new JsonObject();
                        cSurvey.addProperty("createdSurvey", "false");
                        surveyInfo.add("survey", cSurvey);
                        out.println(gson.toJson(surveyInfo));
                    }
                } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(ParticipantServlet.class.getName()).log(Level.SEVERE, null, ex);

                }
                break;
            }
            case "infoSurvey":
            {
                // conn = DBConnection.Connection();
                Participant participant = (Participant) request.getSession().getAttribute("participant");
                
                if (participant == null) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject surveyInfo = new JsonObject();
                    surveyInfo.addProperty("state", "-1");
                    out.println(gson.toJson(surveyInfo)); 
                    break;
                }
              
                
                survey = Survey.getSurvey(participant.getAssembly().getId());
                System.out.println(participant);
                System.out.println(participant.getAssembly());
                System.out.println(participant.getAssembly().getId());
                
                if (survey == null && participant.getStatus() < 2) {
                    System.out.println(survey);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject surveyInfo = new JsonObject();
                    surveyInfo.addProperty("state", "1");
                    out.println(gson.toJson(surveyInfo)); 
                    break;
                }
                
               
                if (survey == null && participant.getStatus() >= 2) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject surveyInfo = new JsonObject();
                    surveyInfo.addProperty("state", "2");
                    out.println(gson.toJson(surveyInfo)); 
                    break;
                }
                
                
                /*  System.out.println("Public Key:");
                System.out.println(convertToPublicKey(encodedPublicKey));
                String token = generateJwtToken(privateKey);
                https://www.wstutorial.com/rest/jwt-java-public-key-rsa.html */
               
                
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject surveyInfo = new JsonObject();
                surveyInfo.addProperty("state", "3");
                surveyInfo.add("Survey", survey.toJson());
                out.println(gson.toJson(surveyInfo));

                break;
            }
            default:
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        service(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        service(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
