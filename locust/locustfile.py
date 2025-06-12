import random
import json
import uuid
from locust import HttpUser, task, between
from locust.exception import StopUser


class BankingAPIUser(HttpUser):
    """
    Locust user that simulates a user interacting with the banking API.
    
    This user will:
    1. Register a new account
    2. Login to get a JWT token
    3. Get wallet information
    4. Perform various types of transactions (topup, debin, p2p)
    5. Check transaction history
    """
    
    wait_time = between(1, 3)  # Wait 1-3 seconds between tasks
    
    def on_start(self):
        """Called when a simulated user starts"""
        self.jwt_token = None
        self.wallet_id = None
        self.username = None
        self.user_id = None
        self.current_balance = 0.0
        
        # Register and login the user
        if self.register_user() and self.login_user():
            self.get_wallet_info()
        else:
            # If registration/login fails, stop this user
            raise StopUser()
    
    def register_user(self):
        """Register a new user account"""
        self.username = f"testuser_{random.randint(1000, 9999)}_{random.randint(100, 999)}"
        self.email = f"{self.username}@example.com"
        self.password = "SecurePassword123!"
        
        registration_data = {
            "username": self.username,
            "email": self.email,
            "password": self.password
        }
        
        response = self.client.post(
            "/auth/register",
            json=registration_data,
            catch_response=True
        )
        
        if response.status_code == 200:
            print(f"✅ User {self.username} registered successfully")
            return True
        else:
            print(f"❌ Registration failed for {self.username}: {response.text}")
            return False
    
    def login_user(self):
        """Login and obtain JWT token"""
        login_data = {
            "username": self.username,
            "password": self.password
        }
        
        response = self.client.post(
            "/auth/login",
            json=login_data,
            catch_response=True
        )
        
        if response.status_code == 200:
            data = response.json()
            self.jwt_token = data.get("token")
            user_data = data.get("userData", {})
            self.user_id = user_data.get("id")
            print(f"✅ User {self.username} logged in successfully")
            return True
        else:
            print(f"❌ Login failed for {self.username}: {response.text}")
            return False
    
    def get_headers(self):
        """Get headers with JWT token for authenticated requests"""
        return {
            "Authorization": f"Bearer {self.jwt_token}",
            "Content-Type": "application/json"
        }
    
    def get_wallet_info(self):
        """Get user's wallet information"""
        with self.client.get(
            "/wallets/user",
            headers=self.get_headers(),
            catch_response=True,
            name="Get Wallet Info"
        ) as response:
            if response.status_code == 200:
                wallet_data = response.json()
                self.wallet_id = wallet_data.get("id")
                self.current_balance = wallet_data.get("balance", 0.0)
                print(f"✅ Wallet info retrieved for {self.username}, balance: ${self.current_balance}")
                response.success()
            else:
                print(f"❌ Failed to get wallet info for {self.username}")
                response.failure(f"Failed to get wallet info: {response.status_code}")
    
    @task(10)
    def check_wallet_balance(self):
        """Check wallet balance (high frequency task)"""
        with self.client.get(
            "/wallets/user",
            headers=self.get_headers(),
            catch_response=True,
            name="Check Balance"
        ) as response:
            if response.status_code == 200:
                wallet_data = response.json()
                self.current_balance = wallet_data.get("balance", 0.0)
                response.success()
            else:
                response.failure(f"Failed to check balance: {response.status_code}")
    
    @task(8)
    def view_transaction_history(self):
        """View transaction history"""
        with self.client.get(
            "/wallets/transactions",
            headers=self.get_headers(),
            catch_response=True,
            name="View Transactions"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed to get transactions: {response.status_code}")
    
    @task(6)
    def topup_wallet(self):
        """Perform external topup to add funds"""
        topup_amount = round(random.uniform(10.0, 100.0), 2)
        
        topup_data = {
            "amount": topup_amount,
            "description": f"Topup via Bank Account {random.randint(1000, 9999)}",
            "externalWalletInfo": "11111111-1111-1111-1111-111111111111"
        }
        
        with self.client.post(
            "/wallets/transactions/topup",
            json=topup_data,
            headers=self.get_headers(),
            catch_response=True,
            name="External Topup"
        ) as response:
            if response.status_code == 200:
                self.current_balance += topup_amount
                print(f"💰 {self.username} topped up ${topup_amount}")
                response.success()
            else:
                response.failure(f"Topup failed: {response.status_code}")
    
    @task(4)
    def debin_transaction(self):
        """Perform external debin transaction"""
        debin_amount = round(random.uniform(5.0, 50.0), 2)
        
        # Use special wallet IDs that are configured in the external service mock
        external_wallets = [
            "11111111-1111-1111-1111-111111111111",  # Accept wallet - should succeed
            "22222222-2222-2222-2222-222222222222",  # Reject wallet - should fail  
            f"random-wallet-{random.randint(1000, 9999)}"  # Random wallet - should fail
        ]
        
        debin_data = {
            "amount": debin_amount,
            "description": f"Payment to merchant {random.randint(100, 999)}",
            "externalWalletInfo": random.choice(external_wallets)
        }
        
        with self.client.post(
            "/wallets/transactions/debin",
            json=debin_data,
            headers=self.get_headers(),
            catch_response=True,
            name="External Debin"
        ) as response:
            if response.status_code == 200:
                self.current_balance += debin_amount  # Debin adds money to wallet
                print(f"🏦 {self.username} debin transaction ${debin_amount}")
                response.success()
            elif response.status_code in [400, 404, 500]:
                # Expected failures based on external wallet configuration
                response.success()
            else:
                response.failure(f"Unexpected debin response: {response.status_code}")
    
    @task(3)
    def p2p_transfer(self):
        """Perform peer-to-peer transfer (requires sufficient balance)"""
        if self.current_balance < 10.0:
            # Skip P2P if insufficient funds
            return
        
        transfer_amount = round(min(random.uniform(1.0, 20.0), self.current_balance * 0.5), 2)
        
        # Generate a random UUID for receiver wallet
        # In a real scenario, this would be another user's wallet
        receiver_wallet_id = str(uuid.uuid4())
        
        p2p_data = {
            "amount": transfer_amount,
            "description": f"Transfer to friend {random.randint(100, 999)}",
            "receiverWalletId": receiver_wallet_id
        }
        
        with self.client.post(
            "/wallets/transactions/p2p",
            json=p2p_data,
            headers=self.get_headers(),
            catch_response=True,
            name="P2P Transfer"
        ) as response:
            if response.status_code == 200:
                self.current_balance -= transfer_amount
                print(f"💸 {self.username} sent ${transfer_amount} to {receiver_wallet_id[:8]}...")
                response.success()
            elif response.status_code == 404:
                # Expected failure - receiver wallet doesn't exist
                response.success()
            elif response.status_code == 400:
                # Expected failure - insufficient funds or other validation
                response.success()
            else:
                response.failure(f"Unexpected P2P response: {response.status_code}")
    
# Configure the test
class RegularUser(BankingAPIUser):
    weight = 10  # Most users are regular users


class PowerUser(BankingAPIUser):
    """Power users perform more transactions"""
    weight = 3
    wait_time = between(0.5, 1.5)  # Faster operations 