/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.persistence.Id;

/**
 *
 * @author avianey
 */
public final class Block {

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Id
    private Integer id;
    private String hash;
    private String previousHash;
    private Survey survey;
    private String data;

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getData() {
        return data;
    }

    public String getDataString() {
        return this.data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    private final long timeStamp;
    
    public Block(Survey survey, String previousHash, String data) {
        this.survey = survey;
        this.data = data;
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }


    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }


    public String decrypt(SecretKey sk) {
 System.out.println("DATA");
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sk);
              System.out.println("DATA");
            System.out.println(Base64.getDecoder().decode(this.data));
            byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(this.data));
            return new String(decryptedData);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void display() {
        System.out.println("Address : " + this.getHash());
        System.out.println("Vote : " + this.getData());
    }

    public String calculateHash() {

        String dataToHash = previousHash
                + Long.toString(timeStamp)
                + data;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] strHash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(strHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void display(PrintStream stream) {
        stream.println("Address : " + this.getHash());
        stream.println("Vote : " + this.getData());

    }
    
    
    
    public static boolean Insert(Connection conn, Block block) throws SQLException {
        //String value="'"+email+"','"+pseudo+"','"+password+"'";
        //String sql = "insert into participants(idUser,idAssembly,title,description,adresse,date, time)) values(?,?,?,?,?,?,?)";
        String sql = "insert into blocks(id_survey,address,previous_address,vote)) values(?,?,?,?)";
        PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); 
        preparedStatement.setString(1, Integer.toString(block.getSurvey().getId()));
        preparedStatement.setString(2, block.getHash());
        preparedStatement.setString(3, block.getPreviousHash());
        preparedStatement.setString(4, block.getData());
        

        int flag = preparedStatement.executeUpdate();

        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                block.setId(generatedKeys.getInt(1));
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

    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public static ArrayList<Block> GetBlocks(Connection conn, Survey survey) throws SQLException {
        //String value="'"+email+"','"+pseudo+"','"+password+"'";
        //String sql = "insert into participants(idUser,idAssembly,title,description,adresse,date, time)) values(?,?,?,?,?,?,?)";
        ArrayList<Block> blocks = new ArrayList<>();
        String sql="select * from blocks where id_survey = ? ";
        PreparedStatement stmt = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);    
        stmt.setInt(1,survey.getId()); 
        
        ResultSet rs = stmt.executeQuery();
        
        
        while(rs.next())
        {
            Integer id = rs.getInt("id_block");
            String address =  rs.getString("address");
            String pAddress = rs.getString("previous_address");
            String data = rs.getString("vote");
            
            Block block = new Block(survey, pAddress, data);
            block.setHash(address);
            blocks.add(block);
        }

        return blocks;
    }
    
}
