/*
 * This source file was generated by the Gradle 'init' task
 */
package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class App {

    static String DB_URL;
    static String DB_USER;
    static String DB_PASSWORD;
    static Map<String, String> dtoMap = null;

    public static void main(String[] args) {

        // 환경 변수 가져오기
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("config.properties 파일을 읽을 수 없습니다: " + e.getMessage());
            return;
        }

        DB_URL = props.getProperty("db.url");
        DB_USER = props.getProperty("db.user");
        DB_PASSWORD = props.getProperty("db.password");




        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare("spring.education.queue", true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                //message dto 형태로 분석
                ObjectMapper objectMapper = new ObjectMapper();
                dtoMap = objectMapper.readValue(message, Map.class);

                updateContent(message);
                buildAndUpdate(message);
            };

            channel.basicConsume("spring.education.queue", true, deliverCallback, consumerTag -> { });
        }catch(IOException e){
            e.printStackTrace();
        }catch(TimeoutException e){
            e.printStackTrace();
        }
    }

    public static void updateContent(String message){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> dtoMap = null;
            dtoMap = objectMapper.readValue(message, Map.class);
            System.out.println(" [x] Received '" + dtoMap.get("service") + "'");

            //현재 경로 확인용 - intellij 와 gradlew 가 달라서 원인 파악용.
            //intellij : Working Directory = /Users/user/github/spring-education/spring-judge/judge
            //graldew : Working Directory = /Users/user/github/spring-education/spring-judge/judge/app
            System.out.println("Working Directory = " + System.getProperty("user.dir"));

            //dtoMap 에서 controller 받은 코드 파일에 덮어쓰기
            String basicFilePath = "../src/main/java/com/spring_education/template";
            String filePath = basicFilePath + "/controller/TestController.java";
            Files.write(Paths.get(filePath), dtoMap.get("controller").getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            //dtoMap 에서 service 받은 코드 파일에 덮어쓰기
            filePath = basicFilePath + "/service/TestService.java";
            Files.write(Paths.get(filePath), dtoMap.get("service").getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }


    }


    public static void buildAndUpdate(String message){

        // 빌드 전에 이미 같은 제출 이력이 있는지 확인
        String controller = dtoMap.get("controller");
        String service = dtoMap.get("service");

        String selectSQL = "select * from problem_submit where user_id = ? and problem_id = ?";
        try(java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(selectSQL)){

            pstmt.setString(1, dtoMap.get("user_id"));
            pstmt.setString(2, dtoMap.get("problem_id"));
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()){
                if(controller.equals(rs.getString("controller_code")) && service.equals(rs.getString("service_code"))){
                    System.out.println("제출한 코드와 같습니다.");
                    return;
                }
            }

        } catch (SQLException e) {
            System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
        }

        // 빌드
        try{
            String gradlewPath = "./gradlew";

            ProcessBuilder processBuilder = new ProcessBuilder(gradlewPath, "build");

            // directory 설정 필수(!!). 이렇게 해야 스프링부트의 graldew 설정하여 잘 작동함
            processBuilder.directory(new File(".."));
            Process process = processBuilder.start();

            // 프로세스의 출력 읽기
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            /*
            출력 예시 일부 ( 성공 시 )
            BUILD SUCCESSFUL in 3s
            7 actionable tasks: 4 executed, 3 up-to-date
            프로세스 종료 코드: 0

            //
            //

            출력 예시 일부 ( 실패 시 )
            Test1 > 출력_확인() FAILED
            java.lang.AssertionError at Test1.java:21
            7 actionable tasks: 5 executed, 2 up-to-date
            프로세스 종료 코드: 1
            */
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            //에러 로그 (gradlew build 하면서)
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            // 프로세스 종료 대기 및 종료 코드 확인
            int exitCode = process.waitFor();
            System.out.println("프로세스 종료 코드: " + exitCode);

            // 채점결과에 따라 db 에 반영하는 부분
            String problemSubmitId = dtoMap.get("problem_submit_id");

            if(exitCode == 0){
                //정답입니다!!
                String updateSQL = "update problem_submit set is_correct = 1 where problem_submit_id = ?";
                try(java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    PreparedStatement pstmt = conn.prepareStatement(updateSQL)){

                    pstmt.setString(1, problemSubmitId);
                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
                }

            }else{
                //틀렸습니다!!
                String updateSQL = "update problem_submit set is_correct = 0 where problem_submit_id = ?";
                try(java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    PreparedStatement pstmt = conn.prepareStatement(updateSQL)){

                    pstmt.setString(1, problemSubmitId);
                    pstmt.executeUpdate();

                } catch (SQLException e) {
                    System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
