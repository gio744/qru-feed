from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    database_url: str = "postgresql://qru:qru@localhost:5432/qru"
    jwt_secret: str = "CHANGE_ME_IN_PRODUCTION"
    jwt_issuer: str = "qru-transito"
    env: str = "development"
    release_signing_secret: str = "LEGACY_DEV_ONLY"
    release_public_key_b64: str = ""
    model_config = SettingsConfigDict(env_prefix="QRU_", env_file=".env", extra="ignore")

settings = Settings()
