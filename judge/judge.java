import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.io.InputStreamReader;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class judge {
    public static void main(String[] args){

        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare("spring.education.queue", true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");

                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> dtoMap = objectMapper.readValue(message, Map.class);
                System.out.println(" [x] Received '" + dtoMap.get("service") + "'");
                buildSpring();
            };

            channel.basicConsume("spring.education.queue", true, deliverCallback, consumerTag -> { });
        }catch(IOException e){
            e.printStackTrace();
        }catch(TimeoutException e){
            e.printStackTrace();
        }
    }


    public static void buildSpring(){
        try{
            String gradlewPath = "../gradlew";
            ProcessBuilder processBuilder = new ProcessBuilder(gradlewPath, "build");

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

            // 프로세스 종료 대기 및 종료 코드 확인
            int exitCode = process.waitFor();
            System.out.println("프로세스 종료 코드: " + exitCode);
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
