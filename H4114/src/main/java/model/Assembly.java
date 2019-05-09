/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import javax.persistence.Id;
import service.Util;
import com.google.gson.JsonObject;

/**
 *
 * @author avianey
 */
public class Assembly {


    @Id
    private Integer id;
    private Survey currentSurvey;
    private String colour;
    private String title;
    private Date date;
    private Time time;
    private String description;
    private int radio;
    private double latitude;
    private double longitude;

    public Assembly(Integer id, String title, String description,Date date, int radio, String colour, double latitude, double longitude) {
        this.id = id;
        this.currentSurvey = null;
        this.colour = "red";
        this.radio = radio;
        this.latitude = latitude;
        this.longitude = longitude;

        this.date = date;

        this.title = title;
        this.description = description;

        this.colour = colour;
    
        // this.latitiude = getlat

    }

    @Override
    public String toString() {
        return "{id:" + id + ", "
                + "colour:" + colour + ", "
                + "title:" + title + ", "
                + "description:" + description + ","
                + " radio:" + radio + ","
                + " latitude:" + latitude + ","
                + " longitude:" + longitude+"}";
    }
    
    public JsonObject toJson(){
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("colour", this.colour);
        json.addProperty("title", this.title);
        json.addProperty("description", this.description);
        json.addProperty("radio", this.radio);
        json.addProperty("latitude", this.latitude);
        json.addProperty("longitude", this.longitude);
        return json;
    }
    
    

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRadio() {
        return radio;
    }

    public void setRadio(int radio) {
        this.radio = radio;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Survey getCurrentSurvey() {
        return currentSurvey;
    }

    public void addSurvey(Survey survey) {
       
        if (survey == null) {
            return;
        }
        else if (currentSurvey == null ) {
            currentSurvey = survey;
        }
        else if (currentSurvey.stat == 2)
        {
            Survey.removeSurvey(currentSurvey);
            currentSurvey = survey;
        }
    }

    public void startSurvey() {
        if (currentSurvey != null && currentSurvey.stat == 1) {
            currentSurvey.start();
        }
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setCurrentSurvey(Survey currentSurvey) {
        this.currentSurvey = currentSurvey;
    }

    public static Assembly getAssembly(Connection conn, Integer idAssembly) throws SQLException  {
        String sql = "select * from assemblies where id_assembly = ? ";
        PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, idAssembly);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            Assembly assembly = new Assembly(
                   idAssembly,
                    rs.getString("title"),
                    rs.getString("description"),
                    Util.StringToDate(rs.getString("date_time")),
                    rs.getInt("radio"),
                    rs.getString("colour"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
            );
            
            Survey survey = Survey.getSurvey(idAssembly);
            assembly.setCurrentSurvey(survey);

            return assembly;
        }
        return null;
    }
    
    public static boolean Insert(Connection conn, Assembly assembly) throws SQLException {
        
        String sql = "insert into assemblies(title,description,date_time,radio,colour,latitude, longitude) values(?,?,?,?,?,?,?)";
        PreparedStatement preparedStatement = conn.prepareStatement(sql,  Statement.RETURN_GENERATED_KEYS); 
        preparedStatement.setString(1, assembly.getTitle());
        preparedStatement.setString(2, assembly.getDescription());
        preparedStatement.setString(3, Util.DateToString(assembly.getDate()));
        preparedStatement.setInt(4, assembly.getRadio());
        preparedStatement.setString(5, assembly.getColour());
        preparedStatement.setDouble(6, assembly.getLatitude());
        preparedStatement.setDouble(7, assembly.getLongitude());

        int flag = preparedStatement.executeUpdate();

        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                assembly.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }

        if (flag != -1) {
            return true;
        } else {
            return false;
        }
}
    
    public static ArrayList<Assembly> GetAssemblies(Connection conn) throws SQLException {
        //String value="'"+email+"','"+pseudo+"','"+password+"'";
        //String sql = "insert into participants(idUser,idAssembly,title,description,adresse,date, time)) values(?,?,?,?,?,?,?)";
        ArrayList<Assembly> assemblies = new ArrayList<>();
        String sql="select * from assemblies";
        PreparedStatement stmt = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);            
        ResultSet rs = stmt.executeQuery();

        while(rs.next())
        {
            Assembly assembly = new Assembly(
                    rs.getInt("id_assembly"),
                    rs.getString("title"),
                    rs.getString("description"),
                    Util.StringToDate(rs.getString("date_time")),
                    rs.getInt("radio"),
                    rs.getString("colour"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
            );
            
            
            assemblies.add(assembly);
        }

        return assemblies;
    }

}
