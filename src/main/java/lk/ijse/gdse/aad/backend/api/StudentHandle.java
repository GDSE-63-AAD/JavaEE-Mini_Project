package lk.ijse.gdse.aad.backend.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lk.ijse.gdse.aad.backend.dto.StudentDTO;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;

@WebServlet(urlPatterns = "/student")
public class StudentHandle extends HttpServlet {
    Connection connection;
    private static final String SaveStudentData = "INSERT INTO student(name,email,city,level) VALUES (?,?,?,?)";


    @Override
    public void init() throws ServletException {

//        try {
//            Class.forName(getServletContext().getInitParameter("mysql-driver"));
//            String username = getServletContext().getInitParameter("db-user");
//            String password = getServletContext().getInitParameter("db-pw");
//            String url = getServletContext().getInitParameter("db-url");
//            this.connection = DriverManager.getConnection(url,username,password);
//
//        } catch (ClassNotFoundException | SQLException ex) {
//            throw new RuntimeException(ex);
//        }
        try {
            InitialContext ctx = new InitialContext();
            DataSource pool = (DataSource) ctx.lookup("java:comp/env/jdbc/student");
            this.connection = pool.getConnection();


        } catch (NamingException | SQLException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
             if(req.getContentType() == null || !req.getContentType().toLowerCase().startsWith("application/json")){
                 rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
             }
        try {

        Jsonb jsonb = JsonbBuilder.create();
        StudentDTO studentObj = jsonb.fromJson(req.getReader(), StudentDTO.class); 
        //validation
        if(studentObj.getName() == null || !studentObj.getName().matches("[A-Za-z ]+")){
            throw new RuntimeException("Invalid Name");
        } else if (studentObj.getCity() == null || !studentObj.getCity().matches("[A-Za-z ]+")) {
            throw new RuntimeException("Invalid City");
        } else if (studentObj.getEmail()==null) {
            throw new RuntimeException("Invalid Email");
        } else if (studentObj.getLevel() <= 0) {
            throw new RemoteException("Invalid Level");
        }
        //save data in db

            PreparedStatement ps =
                    connection.prepareStatement(SaveStudentData, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,studentObj.getName());
            ps.setString(2,studentObj.getCity());
            ps.setString(3,studentObj.getEmail());
            ps.setInt(4,studentObj.getLevel());

            if(ps.executeUpdate() != 1){
                throw new RuntimeException("Save Failed");
            }
            ResultSet rst = ps.getGeneratedKeys();
            rst.next();
            rsp.setStatus(HttpServletResponse.SC_CREATED);
            //the created json is sent to frontend
            rsp.setContentType("application/json");
            jsonb.toJson(studentObj,rsp.getWriter());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Todo:Exception Handle

    }
}
