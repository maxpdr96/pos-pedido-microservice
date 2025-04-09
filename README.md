---

# Sistema de Pedidos e Entregas - Documentação Técnica

## Pré-requisitos Iniciais

### 1. Criação de Entregadores (Passo Essencial)

- **Endpoint:** `POST /entregadores` (microserviço de Gestão de Entregas)
- **Permissão:** `ADMIN`
- **Corpo da Requisição:**

```json
{
  "nome": "João Silva",
  "veiculo": "Moto Honda CG 160",
  "status": "DISPONIVEL"
}
```

> **Observação:** Sem entregadores cadastrados e disponíveis, as entregas do tipo `NORMAL` e `EXPRESSA` não poderão ser criadas.

---

## Fluxo 1: Criação de Pedido e Entrega Automática

### 1. Cliente cria um pedido

- **Endpoint:** `POST /pedidos` (microserviço de Gestão de Pedidos)
- **Corpo da Requisição:**

```json
{
  "clienteId": 1,
  "enderecoEntrega": "Rua A, 123 - Centro",
  "tipoEntrega": "NORMAL",
  "itens": [
    {
      "produto": "Pizza de Calabresa",
      "quantidade": 1,
      "precoUnitario": 45.90
    }
  ]
}
```

### 2. Comunicação entre microserviços (automática)

- O microserviço de Pedidos:
    - Cria o pedido
    - Calcula o valor total
    - Chama o microserviço de Entregas via FeignClient
- **Endpoint chamado:** `POST /entregas`
- **Corpo enviado:**

```json
{
  "pedidoId": 1,
  "tipo": "NORMAL",
  "enderecoEntrega": "Rua A, 123 - Centro",
  "valorPedido": 45.90,
  "observacoes": "..."
}
```

### 3. Microserviço de Entregas

- Seleciona um entregador disponível (Strategy Pattern)
- Calcula valor e tempo da entrega
- Cria o registro da entrega
- Atualiza o entregador para `EM_ENTREGA`

### 4. Comunicação de volta para Pedidos

- **Endpoint:** `PUT /pedidos/{id}/status`
- **Corpo enviado:**

```json
{
  "status": "EM_TRANSPORTE"
}
```

---

## Fluxo 2: Atualização de Status da Entrega

### 1. Entregador atualiza status para EM_ROTA

- **Endpoint:** `PUT /entregas/{id}/status`
- **Corpo:**

```json
{
  "status": "EM_ROTA"
}
```

### 2. Entregador conclui a entrega

- **Endpoint:** `PUT /entregas/{id}/status`
- **Corpo:**

```json
{
  "status": "ENTREGUE"
}
```

### 3. Comunicação entre microserviços

- Atualiza entrega para `ENTREGUE`
- Libera entregador (`DISPONIVEL`)
- **Chamada:** `PUT /pedidos/{id}/status`
- **Corpo:**

```json
{
  "status": "ENTREGUE"
}
```

---

## Fluxo 3: Cancelamento de Entrega

### 1. Cancelamento

- **Endpoint:** `PUT /entregas/{id}/status`
- **Corpo:**

```json
{
  "status": "CANCELADA"
}
```

### 2. Comunicação entre microserviços

- Atualiza entrega para `CANCELADA`
- Libera entregador (se houver)
- **Chamada:** `PUT /pedidos/{id}/status`
- **Corpo:**

```json
{
  "status": "CANCELADO"
}
```

---

## Fluxo 4: Retirada pelo Cliente

### 1. Pedido com tipo `RETIRADA`

- **Endpoint:** `POST /pedidos`
- **Corpo:**

```json
{
  "clienteId": 1,
  "enderecoEntrega": "Rua A, 123 - Centro",
  "tipoEntrega": "RETIRADA",
  "itens": [...]
}
```

### 2. Comunicação entre microserviços

- Não seleciona entregador
- Valor da entrega: **zero**
- Tempo de preparação: **15 min**
- Não atualiza status para `EM_TRANSPORTE`

### 3. Pedido pronto para retirada

- **Endpoint:** `PUT /entregas/{id}/status`
- **Corpo:**

```json
{
  "status": "ENTREGUE"
}
```

---

## Observações Importantes

- **Sequência obrigatória:** Criar pelo menos 1 entregador antes de iniciar o sistema
- **Entrega EXPRESSA:** Requer veículo com "moto" na descrição
- **Propagação de Token:** JWT propagado automaticamente entre os microserviços via FeignClient
- **Strategy Pattern:** Personaliza lógica por tipo de entrega:
    - Seleção de entregador
    - Cálculo de valor
    - Estimativa de tempo

---