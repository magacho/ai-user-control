#!/bin/bash

# Carregar variáveis de ambiente
source .env

# Usar Java 21
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 21.0.9-tem

# Executar aplicação
mvn spring-boot:run
