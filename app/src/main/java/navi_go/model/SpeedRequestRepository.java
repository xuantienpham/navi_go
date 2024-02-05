package navi_go.model;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

import navi_go.util.Result;

public class SpeedRequestRepository extends RequestRepository {
    public SpeedRequestRepository(Executor executor) {
        super(executor);
    }

    public Result<String> makeSynchronousRequest(String urlString) {
        // HttpURLConnection logic
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            InputStream inputStream = httpConnection.getInputStream();

            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, bytesRead));
            }
            String result = stringBuilder.toString();

            //JSONObject jsonOSM = new JSONObject(result);

            Result.Success<String> stringSuccess = new Result.Success<>(result);
            inputStream.close();
            return stringSuccess;
        } catch (Exception e) {
            return new Result.Error<>(e);
        }
    }
}