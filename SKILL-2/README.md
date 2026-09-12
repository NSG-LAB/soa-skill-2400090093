# E-Commerce API Gateway Project

## SOA Programming and Microservices

This project demonstrates:

- Product Service
- Cart Service
- API Gateway
- Request Routing
- Multiple Product Service Instances
- Load Balancing
- REST API Communication

---

# Architecture

```text
                         CLIENT
                           |
                           v
                    +-------------+
                    | API GATEWAY |
                    |    :8080    |
                    +-------------+
                       /       \
                      /         \
                     v           v
              /products/**     /cart/**
                   |              |
             +-----+-----+        |
             |           |        |
             v           v        v
          Product     Product    Cart
          Instance 1  Instance 2 Service
           :8081        :8083     :8082
