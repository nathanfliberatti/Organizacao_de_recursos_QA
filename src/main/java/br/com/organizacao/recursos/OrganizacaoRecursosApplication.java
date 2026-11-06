package br.com.organizacao.recursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrganizacaoRecursosApplication {
    public static void main(String[] args) {
        if (args.length > 0 && "cli".equals(args[0])) {
            System.exit(br.com.organizacao.recursos.cli.ProductCli.run(
                    java.util.Arrays.copyOfRange(args, 1, args.length)));
            return;
        }
        SpringApplication.run(OrganizacaoRecursosApplication.class, args);
    }
}
