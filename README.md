# MedFlow - Sistema de Gestão de Clínicas Médicas 🏥

Bem-vindo ao **MedFlow**, um sistema completo e moderno construído do zero, passando por todas as fases de uma engenharia de software corporativa: do planejamento e banco de dados, até a integração React+Java e orquestração Docker/Kubernetes com pipelines de CI/CD.

## 🚀 Como Ligar o Sistema Inteiro na Sua Máquina
O projeto foi 100% containerizado para facilitar a sua vida. Esqueça de instalar banco de dados ou mensageria manualmente.

**Pré-requisitos:** Você precisa ter apenas o [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado e rodando.

Abra o terminal na pasta raiz do projeto (`Medflow/`) e digite um único comando:
```bash
docker compose up -d --build
```
*O comando `--build` garante que as imagens do frontend e do backend sejam compiladas com a última versão do seu código.*

Pronto! Em cerca de 30 a 60 segundos, o ecossistema inteiro estará vivo.

## 🧭 Onde acessar cada coisa? (Links Rápidos)

| O que é? | URL de Acesso | O que fazer lá? |
| :--- | :--- | :--- |
| **🌐 Frontend (React)** | [http://localhost:5173](http://localhost:5173) | Esta é a tela que o usuário final vê. Você pode se registrar como Paciente ou Médico, fazer login, buscar médicos, agendar consultas e ver o Dashboard. |
| **📖 API Docs (Swagger)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | O "cérebro" sem a interface gráfica. Aqui você pode testar as chamadas puras da API, verificar todos os endpoints disponíveis e ver os modelos (DTOs) que o Backend espera receber. |
| **🐰 Mensageria (RabbitMQ)** | [http://localhost:15672](http://localhost:15672) | O painel que gerencia eventos assíncronos (como o disparo de "e-mails" ao confirmar consultas). **Usuário:** `guest` / **Senha:** `guest` |
| **📊 Painel Grafana** | [http://localhost:3000](http://localhost:3000) | Observabilidade e saúde do sistema. Acompanhe se o sistema está consumindo muita CPU ou quantas requisições por segundo estão entrando. **Usuário:** `admin` / **Senha:** `admin` |
| **⚙️ Métricas Brutas** | [http://localhost:9090](http://localhost:9090) | Painel do Prometheus. Coleta os dados do Spring Boot Actuator em tempo real para o Grafana ler. |

---

## 🛠️ Como Testar e Ver Funcionar de Verdade?

Siga este passo-a-passo simples para ver todos os módulos conversando entre si:

1. **Acesse o Frontend** ([http://localhost:5173](http://localhost:5173)) e clique em "Criar Conta". 
2. Escolha se cadastrar como `PACIENTE` e faça o login com o e-mail/senha que acabou de criar.
3. Se você não vir médicos disponíveis, é porque o banco está vazio. Abra o **Swagger** ([http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)), cadastre uma nova especialidade (ex: Cardiologia) e um novo médico. *Dica: Se a rota pedir Token, use a rota de login no Swagger, copie o token e clique no botão verde "Authorize" no topo da página.*
4. Volte para o Frontend. Na tela de médicos, você verá o médico que acabou de criar. Clique para **Agendar uma Consulta**.
5. Quando a consulta for agendada:
   - Um registro será salvo no banco **PostgreSQL**.
   - Um evento de notificação será disparado automaticamente para o **RabbitMQ** rodando em background (visível no painel dele).
   - O gráfico de requisições no **Grafana** subirá instantaneamente capturando essa chamada.
   - O painel administrativo bloqueará que outra pessoa marque a consulta no exato mesmo horário para aquele médico (Proteção de Concorrência e Race Condition).

## 🛑 Como Desligar o Sistema?
Quando quiser parar tudo, rodar no terminal (na mesma pasta):
```bash
docker compose down
```
Se quiser apagar também o banco de dados (recomeçar 100% do zero):
```bash
docker compose down -v
```

---

## 🏗️ Para Desenvolvedores (Como mexer no código)

- O **Backend** fica na pasta `/backend`. É um projeto Java 21 com Spring Boot. Para testar sem Docker, basta usar o IntelliJ IDEA e rodar o arquivo `MedflowApplication.java` ou rodar `./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"`. Ele tem validações sólidas baseadas em Tokens JWT de segurança (`@PreAuthorize`).
- O **Frontend** fica na pasta `/frontend`. Usa Node 20, Vite, React e Tailwind CSS. Para desenvolver localmente, basta entrar na pasta e rodar `npm install` seguido de `npm run dev`. Ele rodará de forma relâmpago, interceptando os tokens e passando pelo `axios` para bater na API na porta 8080.
- O **Banco de Dados** usa *Flyway* para versionamento. Todos os esquemas de tabelas ficam documentados e controlados na pasta `backend/src/main/resources/db/migration`.
- Os manifestos do **Kubernetes** ficam na pasta `/k8s/` e o pipeline de **CI/CD** (GitHub Actions) está em `.github/workflows/ci.yml`. Esses arquivos preparam o software para ser jogado na nuvem num ambiente multibilionário tolerante a falhas (Self-Healing e Load Balancing automático).

---

## ☁️ Arquitetura em Nuvem (Fase 19)

Para hospedar o sistema na internet de forma 100% gratuita e com qualidade profissional, o MedFlow foi dividido em microsserviços modernos. Cada peça foi para a sua nuvem especializada:

1. **Vercel (Frontend em React):** É a plataforma líder para hospedar interfaces. Ela pega o nosso código da pasta `frontend/`, compila e distribui para o mundo todo. Cada vez que fazemos um `git push` no GitHub, a Vercel atualiza o site automaticamente sem o usuário final perceber (Zero Downtime).
2. **Render.com (Backend em Java Spring Boot):** É onde mora o "cérebro" do sistema (a pasta `backend/`). Ele baixa a nossa imagem Docker do Java, liga o servidor e fica escutando as requisições que chegam da Vercel. Se ficar ocioso, ele "dorme" para economizar custos, e acorda assim que alguém tenta marcar uma consulta.
3. **Neon.tech (Banco de Dados PostgreSQL):** Bancos de dados tradicionais são caros na nuvem. O Neon é um "Serverless Postgres", ou seja, ele desliga quando não está sendo usado e liga em milissegundos. É lá que ficam salvos os logins, médicos e pacientes. O Render (Java) se conecta ao Neon via uma URL (`jdbc:postgresql://...`).
4. **CloudAMQP (Mensageria RabbitMQ):** O mensageiro assíncrono. Quando uma consulta é marcada, o Render manda um bilhete para o CloudAMQP: *"Avise o médico X"*. O CloudAMQP guarda esse bilhete numa fila e processa sem travar a tela do usuário. É perfeito para sistemas de alta carga.
