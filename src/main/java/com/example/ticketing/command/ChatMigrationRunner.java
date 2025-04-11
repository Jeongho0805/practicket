package com.example.ticketing.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMigrationRunner implements CommandLineRunner {

    private final ChatMigrationService migrationService;

    @Override
    public void run(String... args) {
        try {
            for (String arg : args) {
                if (arg.startsWith("--migrateKey=")) {
                    String key = arg.split("=")[1];
                    if (key.equals("chat")) {
                        migrationService.migrateChatting();
                        log.info("채팅 마이그레이션 완료");
                    }
                }
            }
        } catch (Exception e) {
            log.info("마이그레이션 작업중 에러 발생 : " + e.getMessage());
        }
    }
}
