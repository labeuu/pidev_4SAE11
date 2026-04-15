# Microservices index

Spring Boot services live under [backEnd/Microservices](../../backEnd/Microservices). The **API Gateway** maps each service to a URL prefix (see [../api-gateway.md](../api-gateway.md)).

Naming in the first column matches the product/feature name; the **Doc** column is the on-disk filename (some use kebab-case, e.g. `ticket-service.md`).

| Service | Doc | Port | Gateway prefix |
|---------|-----|------|----------------|
| User | [user.md](user.md) | 8090 | `/user` |
| Project | [project.md](project.md) | 8084 | `/project` |
| Offer | [offer.md](offer.md) | 8082 | `/offer` |
| Contract | [contract.md](contract.md) | 8083 | `/contract` |
| Portfolio | [portfolio.md](portfolio.md) | 8086 | `/portfolio` |
| Review | [review.md](review.md) | 8085 | `/review` |
| Planning | [planning.md](planning.md) | 8081 | `/planning` |
| Notification | [notification.md](notification.md) | 8098 | `/notification` |
| Task | [task.md](task.md) | 8091 | `/task` |
| Gamification | [gamification.md](gamification.md) | 8088 | `/gamification` |
| Vendor | [vendor.md](vendor.md) | 8093 | `/vendor` |
| Ticket service | [ticket-service.md](ticket-service.md) | 8094 | `/ticket` |
| Subcontracting | [subcontracting.md](subcontracting.md) | 8099 | `/subcontracting` |
| AImodel (Spring Boot + Ollama) | [AImodel.md](AImodel.md) | 8095 | `/aimodel` |
| FreelanciaJob | [freelanciajob.md](freelanciajob.md) | 8097 | _No gateway route configured_ |
| Chat | [chat.md](chat.md) | 8096 | _No gateway route configured_ |
| Meeting | [meeting.md](meeting.md) | 8101 | _No gateway route configured_ |

Consolidated ports and databases: [../services-and-ports.md](../services-and-ports.md).
