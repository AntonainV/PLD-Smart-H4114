/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.PrintStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author avianey
 */
public class Survey {

    private static HashMap<Integer, Survey> surveys = new HashMap<Integer, Survey>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private Assembly assembly;
    private String question;
    private ArrayList<String> choices;
    private Map<String, Integer> responses;
    int contestation;
    int timeMillis;
    private Set<byte[]> participants;
    int stat;

    private ArrayList<Block> blockchain;

    public Assembly getAssembly() {
        return assembly;
    }

    public HashMap<Integer, Survey> getSurveys() {
        return surveys;
    }

    public static void addSurvey(Integer idAssembly, Survey survey) {
        surveys.put(idAssembly, survey);
    }
    
    public static void removeSurvey(Survey survey) {
        int idAssembly = survey.getAssembly().getId();
        surveys.remove(idAssembly);
    }

    public static Survey getSurvey(Integer idAssembly) {
        return surveys.get(idAssembly);
    }

    public int getStat() {
        return stat;
    }

    public void setStat(int stat) {
        this.stat = stat;
    }

    public void setTimeMillis(int timeMillis) {
        this.timeMillis = timeMillis;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Survey(String question, String timeMillis) {
        this.id = id;
        this.question = question;
        this.stat = 0;
        this.timeMillis = Integer.parseInt(timeMillis);
        this.choices = new ArrayList<>();
        this.participants = new HashSet<>();
        this.blockchain = new ArrayList<>();
        this.responses = new HashMap<>();
        surveys.put(this.id, this);
    }

    public void start() {
        System.out.println("START");
        this.stat = 1;
        final Survey s = this;
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (s) {
                    try {
                        s.wait(timeMillis);
                        s.stat = 2;
                        s.notifyAll();
                        s.updateResults();
                        s.notifyAll();
                        System.out.println("END");

                    } catch (Exception e) {
                    }
                }
            }
        });

    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id_survey", this.id);
        json.addProperty("question", this.question);
        json.addProperty("choices", this.getChoices());
        json.addProperty("state", this.stat);
        json.addProperty("duration", this.timeMillis);
        JsonArray responsesV = new JsonArray();
        if (this.stat == 2) {
            Set keys = this.responses.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                Object key = it.next();
                Object value = this.responses.get(key);

                JsonObject reponseV = new JsonObject();
                reponseV.addProperty("response", (String) key);
                reponseV.addProperty("value", (int) value);
                responsesV.add(reponseV);
            }
        }
        json.add("responses", responsesV);



        return json;
    }

    public int getTimeMillis() {
        return this.timeMillis;
    }

    public String getChoices() {
        String choices = "";
        for (int i = 0; i < this.choices.size(); i++) {
            choices += this.choices.get(i);

            if (i != this.choices.size() - 1) {
                choices += ";";
            }
        }
        return choices;
    }

    public void addResponseChoice(String response) {
        if (!response.isEmpty()) {
            this.responses.put(response, 0);
            this.choices.add(response);
        }
    }

    public String addAnswerVote(String data, Participant paticipant) {
        String pseudo = paticipant.getUser().getPseudo();
        System.out.println("??? " + data);
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] addressP = mDigest.digest(pseudo.getBytes());
            if (this.participants.contains(addressP)) {
                return "";
            }

            String lastHash = "0";
            if (this.blockchain.size() > 0) {
                lastHash = this.blockchain.get(this.blockchain.size() - 1).getHash();
            }

            if (this.stat == 1) {
                Block block = new Block(this, lastHash, data);
                this.blockchain.add(block);
                this.participants.add(addressP);
                return block.getHash();

            }
            return lastHash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void showResults(PrintStream stream) {
        stream.println(this.question + ":");

        for (int i = 0; i < this.blockchain.size(); i++) {
            stream.println("Vote " + i + " : ");
            Block block = this.blockchain.get(i);
            block.display(stream);
        }
    }

    public void addContestation() {
        this.contestation++;
    }

   
    
    public void updateResults()
    {
        for (int i = 0; i < this.blockchain.size(); i++) {
            Block block = this.blockchain.get(i);
            
            
            String choice = block.getDataString();
            
            System.out.println("CHHHHHOICe " + choice);

            if (responses.containsKey(choice)) {
                Integer nb = responses.get(choice) + 1;
                responses.put(choice, nb);
            } else {
                Integer nb = responses.getOrDefault("", 0) + 1;
                responses.put("", nb);

                System.out.println("White vote");
            }
        }
    }

    public void showClearResults(PrintStream stream) {
        System.out.println(this.question + ": \n");

        for (int i = 0; i < this.blockchain.size(); i++) {
            stream.println("Vote " + i + " : ");
            Block block = this.blockchain.get(i);
            String choice = block.getData();
            
            System.out.println("CHHHHHOICe " + choice);

            if (responses.containsKey(choice)) {
                Integer nb = responses.get(choice) + 1;
                responses.put(choice, nb);
            } else {
                Integer nb = responses.getOrDefault("", 0) + 1;
                responses.put("", nb);

                System.out.println("White vote");
            }

            stream.println("Address : " + block.getHash());
            stream.println("Vote : " + choice);

        }

        stream.println("Bilan :" + responses.toString());
    }

    public void getVote(PrintStream stream, String address) {
        for (int i = 0; i < this.blockchain.size(); i++) {
            if (this.blockchain.get(i).getHash().equals(address)) {
                stream.println(this.blockchain.get(i).getData());
            }
        }
    }

    public void display(PrintStream stream) {
        for (int i = 0; i < this.blockchain.size(); i++) {
            stream.println("Hash for block " + i + " : " + this.blockchain.get(i).getHash());
            stream.println("Data for block " + i + " : " + this.blockchain.get(i).getData());
        }
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public Map<String, Integer> getResponses() {
        return responses;
    }

   

    public void clear() {
        this.blockchain.clear();
        this.choices.clear();
    }

    public Boolean isChainValid() {

        Block currentBlock;
        Block previousBlock;

        for (int i = 1; i < this.blockchain.size(); i++) {
            currentBlock = this.blockchain.get(i);
            previousBlock = this.blockchain.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                System.out.println("Current Hashes not equal");
                return false;
            }

            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                System.out.println("Previous Hashes not equal");
                return false;
            }
        }
        return true;
    }

    public static Survey GetSurvey(Connection conn, String idSurvey) throws SQLException {
        String sql = "select * from surveys where id_survey = ? ";
        PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, Integer.parseInt(idSurvey));
        ResultSet rs = stmt.executeQuery();

        if (rs != null) {
            rs.last();
            Survey survey = new Survey(
                    rs.getString("question"),
                    rs.getString("duration")
            );

            survey.setTimeMillis(Integer.parseInt(rs.getString("time")));

            String[] choices = rs.getString("choices").split(";");
            for (int i = 0; i < choices.length; i++) {
                survey.addResponseChoice(choices[i]);
            }

            return survey;
        }

        return null;
    }

    public static boolean Update(Connection conn, Survey survey) throws SQLException {
        String sql = "select * from surveys where id_survey = ? ";
        PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, survey.getId());
        ResultSet rs = stmt.executeQuery();

        if (rs != null) {
            rs.last();
            survey.setQuestion(rs.getString("question"));
            survey.setTimeMillis(Integer.parseInt(rs.getString("time")));
            survey.clear();
            String[] choices = rs.getString("choices").split(";");
            for (int i = 0; i < choices.length; i++) {
                survey.addResponseChoice(choices[i]);
            }
            return true;
        }

        return false;
    }

    public static boolean Insert(Connection conn, Survey survey) throws SQLException {
        //String value="'"+email+"','"+pseudo+"','"+password+"'";
        String sql = "insert into surveys(question,choices,duration) values(?,?,?)";
        PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, survey.getQuestion());
        preparedStatement.setString(2, survey.getChoices());
        preparedStatement.setInt(3, survey.getTimeMillis());


        int flag = preparedStatement.executeUpdate();

        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                survey.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }
        System.out.println("flag=" + flag);
        if (flag != -1) {
            return true;
        } else {
            return false;
        }
    }

}
