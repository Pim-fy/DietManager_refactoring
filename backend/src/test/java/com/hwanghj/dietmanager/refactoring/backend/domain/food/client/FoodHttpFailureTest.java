package com.hwanghj.dietmanager.refactoring.backend.domain.food.client;
import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiException;
import com.hwanghj.dietmanager.refactoring.backend.domain.food.exception.FoodApiLimitException;
class FoodHttpFailureTest {
    @Test void httpErrorsMalformedBodiesAndRecoveryDoNotExposeKey() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var code=new java.util.concurrent.atomic.AtomicInteger(503);
        server.createContext("/food",exchange->{
            byte[] body=(code.get()==200 ? "{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":[],\"totalCount\":0}}" : "private-key-must-not-leak").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code.get(),body.length);
            try(var output=exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var client=new FoodNutritionClient("private-key-must-not-leak",new ObjectMapper(),HttpClient.newHttpClient(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/food"));
            assertThatThrownBy(()->client.search("밥",1,10)).isInstanceOf(FoodApiException.class)
                .hasMessageNotContaining("private-key-must-not-leak").hasNoCause();
            code.set(429);
            assertThatThrownBy(()->client.search("밥",1,10)).isInstanceOf(FoodApiLimitException.class).hasNoCause();
            code.set(200);
            assertThat(client.search("밥",1,10).getItems()).isEmpty();
        } finally { server.stop(0); }
    }
}
