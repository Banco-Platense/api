# Banking API Load Testing with Locust

This directory contains Locust scripts for load testing the Banking API application.

## Overview

The Locust scripts simulate realistic user behavior including:
- User registration and authentication
- Wallet operations (balance checks, transaction history)
- Various transaction types:
  - External top-ups (adding money from external sources)
  - External DEBIN transactions (receiving money from external sources)
  - Peer-to-peer (P2P) transfers between wallets

## Prerequisites

- Python 3.8 or higher
- Banking API application running (default: http://localhost:8080)
- External services mock running (if testing DEBIN functionality)

## Setup

### 1. Navigate to the locust directory
```bash
cd locust
```

### 2. Create and activate a Python virtual environment
```bash
# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate

# On Windows use:
# venv\Scripts\activate
```

### 3. Install dependencies
```bash
pip install -r requirements.txt
```

## Running the Tests

### Basic Load Test (Web UI)

Start the Locust web interface:
```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Start Locust with web UI
locust -f locustfile.py --host=http://localhost:8080
```

Then open your browser to http://localhost:8089 to access the Locust web UI where you can:
- Set number of users
- Set spawn rate
- Start/stop the test
- View real-time metrics

### Headless Load Test (Command Line)

Run a headless (no web UI) load test:
```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Run headless test
locust -f locustfile.py \
  --host=http://localhost:8080 \
  --users 10 \
  --spawn-rate 2 \
  --run-time 5m \
  --headless
```

### Advanced Configuration with HTML Report

Run with custom parameters and generate HTML report:
```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Run with report generation
locust -f locustfile.py \
  --host=http://localhost:8080 \
  --users 50 \
  --spawn-rate 5 \
  --run-time 10m \
  --headless \
  --html=load-test-report.html
```

## Configuration Options

| Option | Description | Example |
|--------|-------------|---------|
| `--users` | Total number of simulated users | `--users 50` |
| `--spawn-rate` | Rate at which users are spawned (users per second) | `--spawn-rate 5` |
| `--run-time` | How long to run the test | `--run-time 10m` |
| `--host` | Target host URL | `--host http://staging.api.com` |
| `--headless` | Run without web UI | `--headless` |
| `--html` | Generate HTML report | `--html=report.html` |

## User Types

The script includes two user types with different behaviors:

### RegularUser (weight: 10)
- Standard users performing typical banking operations
- Wait time: 1-3 seconds between actions
- Most common user type (77% of traffic)

### PowerUser (weight: 3)
- More active users performing frequent transactions
- Wait time: 0.5-1.5 seconds between actions
- Higher transaction frequency (23% of traffic)

## Test Scenarios

### User Registration Flow
1. Generate unique username and email
2. Register new user account
3. Login to obtain JWT token
4. Get initial wallet information

### Task Distribution
Each user performs these tasks with different frequencies:

| Task | Weight | Description |
|------|--------|-------------|
| Check Balance | 10 | Most frequent operation |
| View Transaction History | 8 | High frequency |
| External Topup | 6 | Medium frequency |
| DEBIN Transaction | 4 | Medium frequency |
| P2P Transfer | 3 | Lower frequency |

### Transaction Testing

#### External Top-up
- Amounts: $10-$100
- Simulates adding money from external bank accounts
- Always successful (mocked external service)

#### External DEBIN
- Amounts: $5-$50
- Tests with different external wallet IDs:
  - `11111111-1111-1111-1111-111111111111`: Always succeeds
  - `22222222-2222-2222-2222-222222222222`: Always fails (simulates rejection)
  - Random UUIDs: Always fail (wallet not found)

#### P2P Transfers
- Amounts: $1-$20 (limited by available balance)
- Uses random receiver wallet IDs
- Expected to fail (wallets don't exist) but tests the validation logic

### Balance Management
- Users track their balance locally to make realistic decisions
- P2P transfers only attempted when sufficient funds available
- Balance updated after successful transactions

## Endpoints Tested

The load test covers these API endpoints:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/auth/register` | User registration |
| POST | `/auth/login` | User authentication |
| GET | `/wallets/user` | Get wallet information |
| GET | `/wallets/transactions` | View transaction history |
| POST | `/wallets/transactions/topup` | External funding |
| POST | `/wallets/transactions/debin` | External payments |
| POST | `/wallets/transactions/p2p` | Peer-to-peer transfers |

## Metrics and Monitoring

Locust tracks several important metrics:

### Response Times
- Authentication endpoints
- Wallet operations
- Transaction creation endpoints

### Success/Failure Rates
- Registration success rate
- Login success rate
- Transaction success rates by type
- Expected vs unexpected failures

### Throughput
- Requests per second (RPS)
- Transactions per second by type
- User operations per second

## Expected Behavior

### Successful Operations
- User registration and login
- Wallet balance checks
- Transaction history retrieval
- External top-up transactions
- DEBIN transactions with accept wallet ID

### Expected Failures
- DEBIN transactions with reject wallet ID or unknown wallets
- P2P transfers to non-existent wallets
- P2P transfers with insufficient funds

## Troubleshooting

### Common Issues

1. **Connection refused**: Ensure the API is running on the specified host/port
2. **Authentication failures**: Check if JWT tokens are being properly obtained and used
3. **High failure rates**: Verify external services are running if testing DEBIN functionality
4. **Import errors**: Make sure virtual environment is activated and dependencies installed

### Debugging

Enable verbose logging:
```bash
locust -f locustfile.py --host=http://localhost:8080 --loglevel DEBUG
```

### Performance Issues

If the API becomes slow under load:
1. Reduce spawn rate: `--spawn-rate 1`
2. Reduce number of users: `--users 10`
3. Increase wait times by modifying the script

## Quick Test Commands

### Development Testing
```bash
# Quick validation (2 users, 1 minute)
source venv/bin/activate
locust -f locustfile.py --host=http://localhost:8080 --users 2 --spawn-rate 1 --run-time 1m --headless
```

### Load Testing
```bash
# Standard load test (50 users, 10 minutes)
source venv/bin/activate
locust -f locustfile.py --host=http://localhost:8080 --users 50 --spawn-rate 5 --run-time 10m --headless --html=report.html
```

### Stress Testing
```bash
# High load test (100 users, 30 minutes)
source venv/bin/activate
locust -f locustfile.py --host=http://localhost:8080 --users 100 --spawn-rate 10 --run-time 30m --headless --html=stress-test.html
```

## Customization

### Modifying User Behavior

Edit `locustfile.py` to:
- Change task weights (higher numbers = more frequent)
- Adjust wait times between actions
- Modify transaction amounts
- Add new endpoints or scenarios

### Example: Increase Top-up Frequency
```python
@task(20)  # Increased from 6 to 20
def topup_wallet(self):
    # ... existing code ...
```

## CI/CD Integration

For automated testing in CI/CD pipelines:

```bash
#!/bin/bash
set -e

# Navigate to locust directory
cd locust

# Set up virtual environment
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Run load test
locust -f locustfile.py \
  --host=http://localhost:8080 \
  --users 20 \
  --spawn-rate 5 \
  --run-time 2m \
  --headless \
  --html=load-test-report.html

echo "Load test completed. Report: load-test-report.html"
```

## Best Practices

1. **Start Small**: Begin with few users and low spawn rate
2. **Use Virtual Environment**: Always activate the Python virtual environment
3. **Monitor Resources**: Watch CPU, memory, and database performance
4. **Realistic Scenarios**: Use realistic data and user behavior patterns
5. **Gradual Scaling**: Increase load gradually to find breaking points
6. **Test Isolation**: Use separate test databases/environments

## Results Interpretation

### Good Performance Indicators
- Response times under 500ms for most operations
- Error rate under 1% for successful scenarios
- Stable performance as load increases
- No memory leaks or resource exhaustion

### Warning Signs
- Response times increasing significantly with load
- High error rates on normally successful operations
- Database connection pool exhaustion
- Memory usage growing continuously

## Deactivating Virtual Environment

When finished testing:
```bash
deactivate
```

## Support

For issues with the load testing setup, check:
1. Virtual environment is properly activated
2. All dependencies are installed
3. API application is running and accessible
4. Database performance metrics
5. External service availability 