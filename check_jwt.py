import hmac
import hashlib
import base64
import sys

def base64url_encode(data):
    return base64.urlsafe_b64encode(data).decode('utf-8').rstrip('=')

def verify_jwt(token, secret):
    parts = token.split('.')
    if len(parts) != 3:
        return False
    header_payload = parts[0] + '.' + parts[1]
    signature = parts[2]
    
    expected_sig = base64url_encode(
        hmac.new(secret.encode('utf-8'), header_payload.encode('utf-8'), hashlib.sha256).digest()
    )
    return expected_sig == signature

token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzIiwiZW1haWwiOiJobUBncmlkZHluYW1pY3MuY29tIiwiYXV0aFZlcnNpb24iOjEsImlzcyI6ImF1dGgtc2VydmljZSIsImp0aSI6IjA2NzgyYmI3LTI1MmUtNGZiNi05NGUzLTBjNmNiNjUyN2JlZCIsInRva2VuX3R5cGUiOiJhY2Nlc3MiLCJhdWQiOlsiYXBpLWdhdGV3YXkiXSwicm9sZXMiOlsiSElSSU5HX01BTkFHRVIiXSwic2NvcGVzIjpbIkRFTUFORF9VUERBVEUiLCJERU1BTkRfU1VCTUlUIiwiREVNQU5EX0NSRUFURSIsIkRFTUFORF9WSUVXIiwiREVNQU5EX1NUQVRVU19UUkFOU0lUSU9OIiwiREVNQU5EX0hNX05PTUlOQVRJT05fREVDSURFIiwiREVNQU5EX1BJUEVMSU5FX1ZJRVciXSwiaWF0IjoxNzgyMTQzODE0LCJleHAiOjE3ODI3NDg2MTR9.M_1NTu8HoFfHsHlG8KdbbLGHB7FDCJgnZhrIfjiNBFE"

secrets = [
    # Hardcoded secret for local testing
    "your_jwt_secret_here",
    "default-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256",
    "changeme-replace-in-production-immediately"
]

for s in secrets:
    if verify_jwt(token, s):
        print("MATCHED WITH SECRET: " + s)
        sys.exit(0)

print("NO MATCH FOUND!")
sys.exit(1)
