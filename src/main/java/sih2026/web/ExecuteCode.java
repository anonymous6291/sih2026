package sih2026.web;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sih2026.database.document.DocumentStorageManager;
import sih2026.database.user.UserRole;
import sih2026.sandbox.SandboxCodeExecutor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestController
public class ExecuteCode {
    private final SandboxCodeExecutor sandboxCodeExecutor;
    private final ObjectMapper jsonParser = new ObjectMapper();

    public ExecuteCode(SandboxCodeExecutor sandboxCodeExecutor) {
        this.sandboxCodeExecutor = sandboxCodeExecutor;

        jsonParser.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostMapping("/execute")
    public ResponseEntity<String> executeCode(Authentication authentication, @RequestBody RequestJson requestJson) {
        try {
            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r != null && r.startsWith("ROLE_") ? r.substring(5) : r)
                    .findFirst()
                    .orElse(null);

            String username = authentication.getName();
            UserRole userRole = UserRole.valueOf(role);

            SandboxCodeExecutor.Result result = executeCode0(
                    username,
                    userRole,
                    requestJson.language(),
                    requestJson.base64_code(),
                    requestJson.documents()
            );

            return ResponseEntity.ok(jsonParser.writeValueAsString(result));
        } catch (Exception _) {
            return ResponseEntity.badRequest().build();
        }
    }

    private SandboxCodeExecutor.Result executeCode0(
            String username,
            UserRole userRole,
            String language,
            String base64_code,
            List<DocumentStorageManager.DocumentNameAndId> documents
    ) {
        try {
            return sandboxCodeExecutor.executeCode(username, userRole, language, base64_code, documents);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/testExecute")
    public ResponseEntity<Void> testExecute() {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            Base64.Encoder encoder = Base64.getEncoder();

            String javaCode = """
                    
                    import com.fasterxml.jackson.databind.DeserializationFeature;
                    import com.fasterxml.jackson.databind.ObjectMapper;
                    import org.springframework.http.ResponseEntity;
                    import org.springframework.security.core.Authentication;
                    import org.springframework.security.core.GrantedAuthority;
                    import org.springframework.web.bind.annotation.GetMapping;
                    import org.springframework.web.bind.annotation.PostMapping;
                    import org.springframework.web.bind.annotation.RequestBody;
                    import org.springframework.web.bind.annotation.RestController;
                    public class Run {
                        public static void main(String[] args) {
                            IO.println("Hello Worlsssssssssssssd!!!!");
                        }
                    }
                    """;

            String pythonCode = """
                    print("He111111111111111llo")
                    """;

            String java64 = encoder.encodeToString(javaCode.getBytes(StandardCharsets.UTF_8));
            String python64 = encoder.encodeToString(pythonCode.getBytes(StandardCharsets.UTF_8));

            SandboxCodeExecutor.Result result = executeCode0("a1", UserRole.ADMIN, "java", java64, List.of());

            if (result.error()) {
                IO.println("Error: " + result.message());
            } else {
                IO.println("Success output: ");
                for (SandboxCodeExecutor.ExecutorIntermediateOutput x : result.output().executorIntermediateOutputs()) {
                    IO.println("Error: " + x.error());
                    IO.println("Output: " + new String(decoder.decode(x.base64_message())));
                }
            }

            Thread.sleep(200);

            result = executeCode0("a1", UserRole.ADMIN, "python", python64, List.of());

            if (result.error()) {
                IO.println("Error: " + result.message());
            } else {
                IO.println("Success output: ");
                for (SandboxCodeExecutor.ExecutorIntermediateOutput x : result.output().executorIntermediateOutputs()) {
                    IO.println("Error: " + x.error());
                    IO.println("Output: " + new String(decoder.decode(x.base64_message())));
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    public record RequestJson(String language, String base64_code,
                              List<DocumentStorageManager.DocumentNameAndId> documents) {
    }
}
