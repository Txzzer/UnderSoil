package network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class SensorDataDeserializer implements JsonDeserializer<SensorData> {
    @Override
    public SensorData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) {
            return new SensorData();
        }

        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            if (array.size() > 0) {
                return new Gson().fromJson(array.get(0), SensorData.class);
            }
            return new SensorData();
        } else if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();

            // Check if wrapped in "data", "value", or "result"
            if (obj.has("data") && !obj.get("data").isJsonNull()) {
                JsonElement dataElement = obj.get("data");
                if (dataElement.isJsonObject()) {
                    return new Gson().fromJson(dataElement, SensorData.class);
                } else if (dataElement.isJsonArray()) {
                    JsonArray array = dataElement.getAsJsonArray();
                    if (array.size() > 0) {
                        return new Gson().fromJson(array.get(0), SensorData.class);
                    }
                }
            } else if (obj.has("value") && !obj.get("value").isJsonNull()) {
                JsonElement valElement = obj.get("value");
                if (valElement.isJsonObject()) {
                    return new Gson().fromJson(valElement, SensorData.class);
                } else if (valElement.isJsonArray()) {
                    JsonArray array = valElement.getAsJsonArray();
                    if (array.size() > 0) {
                        return new Gson().fromJson(array.get(0), SensorData.class);
                    }
                }
            }

            return new Gson().fromJson(obj, SensorData.class);
        }
        return new SensorData();
    }
}
