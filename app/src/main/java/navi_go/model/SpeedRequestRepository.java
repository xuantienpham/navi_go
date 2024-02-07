package navi_go.model;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class SpeedRequestRepository extends HttpGetRequestRepository {
    public SpeedRequestRepository(Executor executor) {
        super(executor);
    }

    public Result<String> makeSyncRequest(String urlString) {
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

            //Result.Success<String> stringSuccess = new Result.Success<>(result);
            inputStream.close();
            //return stringSuccess;
            return  null;
        } catch (Exception e) {
            //return new Result.Error<>(e);
            return null;
        }
    }
}