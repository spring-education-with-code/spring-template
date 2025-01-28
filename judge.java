import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class judge {
    public static void main(String[] args){
        String gradlewPath = "./gradlew";

        ProcessBuilder processBuilder = new ProcessBuilder(gradlewPath, "build");

        try {

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
            Thread.currentThread().interrupt();
        }
    }
}
