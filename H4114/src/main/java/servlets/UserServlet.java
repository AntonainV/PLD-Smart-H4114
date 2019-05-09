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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Assembly;
import model.Participant;
import model.User;
import service.DBConnection;

/**
 *
 * @author Arthur
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/UserServlet"})
public class UserServlet extends HttpServlet {

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
        String action=request.getParameter("action"); 
        Connection conn=null;
        String email,password;
        User user;
        Participant participant;
        switch(action){
            case "clearData":
                user = (User)request.getSession().getAttribute("user");
                if(user == null)
                    break;
                Integer id = user.getId();
                try {
                    conn = DBConnection.Connection();
                    String sql="delete from participants where id_user = ? ";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, id); 
	            stmt.executeUpdate();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SQLException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                /*request.getSession().removeAttribute("user");*/
                request.getSession().removeAttribute("participant");
                break;
            case "connect":
            {
                try {
                    email=request.getParameter("email");
                    password=request.getParameter("password");
                    JsonObject connection=new JsonObject();
                    JsonObject connect=new JsonObject();
                    conn = DBConnection.Connection();
                    user = User.Connect(conn, email, password);
                   
                    
                    
                    try (PrintWriter out = response.getWriter()) {
                        Gson gson=new GsonBuilder().setPrettyPrinting().create();
                        if(user != null){
                            
                           
                            
                            connect.addProperty("connect", "successful");
                            request.getSession().setAttribute("user", user);
                        }
                        else{
                            connect.addProperty("connect", "failed");
                        }
                        connection.add("connect", connect);
                        out.println(gson.toJson(connection));
                    }
                }
                catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }   
            case "inscription":
            {
                email=request.getParameter("email");
                String pseudo=request.getParameter("pseudo");
                password=request.getParameter("password");
                JsonObject inscription=new JsonObject();
                try {
                    conn = DBConnection.Connection();
                    boolean exist=User.UserExist(conn, email,pseudo);

                    if(!exist){
                        user = new User(email,password, pseudo);
                        if(User.Insert(conn, user)){
                         
                            try (PrintWriter out = response.getWriter()) {
                                Gson gson=new GsonBuilder().setPrettyPrinting().create();
                                JsonObject inscrit=new JsonObject();
                                inscrit.addProperty("inscrit", "true");
                                inscription.add("inscrit", inscrit);
                                out.println(gson.toJson(inscription));
                            }
                        }
                        else{
                            try (PrintWriter out = response.getWriter()) {
                                Gson gson=new GsonBuilder().setPrettyPrinting().create();
                                JsonObject inscrit=new JsonObject();
                                inscrit.addProperty("inscrit", "false");
                                inscription.add("inscrit", inscrit);
                                out.println(gson.toJson(inscription));
                            }
                        }
                    }
                    else{
                        try (PrintWriter out = response.getWriter()) {
                            Gson gson=new GsonBuilder().setPrettyPrinting().create();
                            JsonObject inscrit=new JsonObject();
                            inscrit.addProperty("inscrit", "false");
                            inscription.add("inscrit", inscrit);
                            out.println(gson.toJson(inscription));
                        }
                    }
                } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                break;
            }
            
                
            case "profil":
            {
                try {
                    email=(String)request.getSession().getAttribute("email");
                    PrintWriter out = response.getWriter();
                    Gson gson=new GsonBuilder().setPrettyPrinting().create();
                    ResultSet rs;
                    conn = DBConnection.Connection();
                    rs = User.FindUserWithEmail(conn, email);
                    JsonObject jsonCompte=new JsonObject();
                    while (rs.next()){
                        jsonCompte.addProperty("id_user", rs.getString(1));
                        jsonCompte.addProperty("email", rs.getString(2));
                        jsonCompte.addProperty("pseudo", rs.getString(3));
                    }
                    rs.beforeFirst();
                    JsonObject container=new JsonObject();
                    container.add("profil", jsonCompte);
                    out.println(gson.toJson(container));
                } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }
            
            case "getParticipants":
            {
                try
                {
                    PrintWriter out = response.getWriter();
                    Gson gson=new GsonBuilder().setPrettyPrinting().create();
                    conn = DBConnection.Connection();
                    ArrayList<Participant> participants = Participant.GetParticipants(conn);
                    JsonObject reponse = new JsonObject();
                    JsonArray jsonListe = new JsonArray();
                    user = (User) request.getSession().getAttribute("user");
                    if (user == null)
                    {
                        break;
                    }
                    for (int i = 0; i < participants.size(); i++)
                    {
                        if (user.getId() != participants.get(i).getId())
                        {
                            jsonListe.add(participants.get(i).toJson());
                        }
                       
                    }
                    reponse.add("Participants", jsonListe);
                    out.println(gson.toJson(reponse));
                    
                } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
                
                
                
                
                break;       
            }
            
            case "getAssemblies":
            {
                try
                {
                    PrintWriter out = response.getWriter();
                    Gson gson=new GsonBuilder().setPrettyPrinting().create();
                    conn = DBConnection.Connection();
                    ArrayList<Assembly> assemblies = Assembly.GetAssemblies(conn);
                    JsonObject reponse = new JsonObject();
                    JsonArray jsonListe = new JsonArray();
                    for (int i = 0; i < assemblies.size(); i++)
                    {
                        jsonListe.add(assemblies.get(i).toJson());
                       
                    }
                    reponse.add("Assemblies", jsonListe);
                    out.println(gson.toJson(reponse));
                    
                } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
               
                break;
            }
            case "getAssemblySession":
            {
                try
                {
                    PrintWriter out = response.getWriter();
                    Gson gson=new GsonBuilder().setPrettyPrinting().create();
                    conn = DBConnection.Connection();
                    participant = (Participant) request.getSession().getAttribute("participant");
                    JsonObject reponse = new JsonObject();
                    JsonObject assembly = new JsonObject();
                    if (participant != null)
                    {
                        reponse.addProperty("exist", true);
                        assembly.addProperty("id_assembly", participant.getAssembly().getId());
                        assembly.addProperty("title", participant.getAssembly().getTitle());
                        assembly.addProperty("colour", participant.getAssembly().getColour());
                    }
                    else
                    {
                        reponse.addProperty("exist", false);
                    }
                   
                  
                   
                    reponse.add("Assembly", assembly);
                    out.println(gson.toJson(reponse));
                    
                } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
               
                break;
            }
             
            
             
            case "joinAssembly":
            {
                try {
                    JsonObject joinAssembly =new JsonObject();
                    conn = DBConnection.Connection();
                    
                    user = (User) request.getSession().getAttribute("user");
                    if(user == null)
                    {
                        break;
                    }
                    participant = (Participant) request.getSession().getAttribute("participant");
                    String idAssembly = request.getParameter("id_assembly");   
                    Assembly assembly;
                    boolean isAlreadyPartAssembly;
                  
                    isAlreadyPartAssembly = User.IsPartAssembly(conn,user);
                    assembly = Assembly.getAssembly(conn, Integer.parseInt(idAssembly));
                    if (isAlreadyPartAssembly || participant != null)
                    {
                        break;
                    }
  
                    double latitude = Double.parseDouble(request.getParameter("latitude"));
                    double longitiude = Double.parseDouble(request.getParameter("longitude"));
                    int status = Integer.parseInt(request.getParameter("status"));
                
                    if (user == null)
                    {
                        break;
                    }
                                   
                    participant = new Participant(  user,
                            assembly,
                            latitude,
                            longitiude,
                            status);

                if(Participant.Insert(conn, participant)){
                    
                    PrintWriter out = response.getWriter();
                    Gson gson=new GsonBuilder().setPrettyPrinting().create();
                    JsonObject participate=new JsonObject();
                    participate.addProperty("participate", "true");
                    joinAssembly.add("participate", participate);
                    
                    request.getSession().setAttribute("participant", participant);
                    
                    JsonObject assemblyJ = new JsonObject();
                    
  
                    assemblyJ.addProperty("id", participant.getAssembly().getId());
                    assemblyJ.addProperty("title", participant.getAssembly().getTitle());
                    assemblyJ.addProperty("colour", participant.getAssembly().getColour());

                    joinAssembly.add("Assembly", assemblyJ);
                    out.println(gson.toJson(joinAssembly));
                    
                    
                }
                else{
                    try (PrintWriter out = response.getWriter()) {
                        Gson gson=new GsonBuilder().setPrettyPrinting().create();
                        JsonObject participate=new JsonObject();
                        participate.addProperty("participate", "false");
                        joinAssembly.add("participate", participate);
                        out.println(gson.toJson(joinAssembly));
                    }
                }
            } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            break;
            }
            case "getPseudos":
            {
                
                JsonObject getPseudo =new JsonObject();
                user = (User) request.getSession().getAttribute("user");
                if (user == null)
                {
                    break;
                }
                String pseudoU = user.getPseudo();
                
                participant = (Participant) request.getSession().getAttribute("participant");
                if (participant != null)
                {
                    
                    int statusU = participant.getStatus();
                    
                    try (PrintWriter out = response.getWriter()) {
                        Gson gson=new GsonBuilder().setPrettyPrinting().create();
                        JsonObject parti =new JsonObject();
                        
                        parti.addProperty("pseudo", pseudoU);
                        parti.addProperty("status", statusU);
                        getPseudo.add("participate", parti);
                        
                        out.println(gson.toJson(getPseudo));
                    }
                }
                else
                {
                    try (PrintWriter out = response.getWriter()) {
                        Gson gson=new GsonBuilder().setPrettyPrinting().create();
                        JsonObject parti =new JsonObject();
                        
                        parti.addProperty("pseudo", pseudoU);
                        parti.addProperty("status", -1);
                        getPseudo.add("participate", parti);
                        
                        out.println(gson.toJson(getPseudo));
                    }
                    
                }
                
            break;
            }
            case "createAssembly":
            {  
                try {
                    
                    conn = DBConnection.Connection();
                    
                    user = (User) request.getSession().getAttribute("user");

                    participant = (Participant) request.getSession().getAttribute("participant");
                    
                    Gson gson=new GsonBuilder().setPrettyPrinting().create();
                    PrintWriter out = response.getWriter() ;
                    JsonObject reponse = new JsonObject();
                    if (participant != null)
                    {
                        System.out.println("sdasdasdsadasfvzxc");
                        reponse.addProperty("created", false);
                        out.println(gson.toJson(reponse));
                        break;
                    }
                    
                    String title = request.getParameter("title");  
                    String description = request.getParameter("description");  
                    String colour = request.getParameter("colour"); 
                    Date date_time = new Date();
                    int radio = Integer.parseInt(request.getParameter("radio"));
                    double latitude = Double.parseDouble(request.getParameter("latitude"));
                    double longitude = Double.parseDouble(request.getParameter("longitude"));
                                        
                    
                    Assembly assembly = new Assembly(0, title, description, date_time, radio, colour, latitude, longitude);
                   
                
                    if (user == null)
                    {
                        System.out.println("asdsadasdsdasdasdsadasfvzxc");
                        reponse.addProperty("created", false);
                        out.println(gson.toJson(reponse));
                        break;
                    }
                                   
                    participant = new Participant(  user,
                            assembly,
                            latitude,
                            longitude,
                            2);
                    

                    if(Assembly.Insert(conn, assembly)){
                            request.getSession().setAttribute("assembly", assembly);
                    }
                    else{
                        System.out.println("dsdasdasdsadasfvzxc");
                        reponse.addProperty("created", false);
                        out.println(gson.toJson(reponse));
                        break;
                    }
                     
                   
                    if(Participant.Insert(conn, participant))
                    {
                       
                        System.out.println("1dsasdsadasfvzxc");
                        JsonObject assemblyJ = new JsonObject();
                        reponse.addProperty("created", true);
                        assemblyJ.addProperty("id_assembly", participant.getAssembly().getId());
                        assemblyJ.addProperty("title", participant.getAssembly().getTitle());
                        assemblyJ.addProperty("colour", participant.getAssembly().getColour());
                        reponse.add("Assembly", assemblyJ);
                        
                        request.getSession().setAttribute("participant", participant);
                    }
                    else
                    {
                        System.out.println("zdsdasdasdsadasfvzxc");
                        reponse.addProperty("created", false);
                    }

                    out.println(gson.toJson(reponse));
                out.close();
            } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(UserServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
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
