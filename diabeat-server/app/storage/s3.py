from .base import StorageProvider
import boto3
from botocore.exceptions import ClientError
from app.config import settings

class S3StorageProvider(StorageProvider):
    """AWS S3 存储实现"""
    
    def __init__(self, bucket_name: str, aws_access_key_id: str, aws_secret_access_key: str, region: str):
        """
        Args:
            bucket_name: S3 桶名称
            aws_access_key_id: AWS 访问密钥 ID
            aws_secret_access_key: AWS 密钥
            region: AWS 区域
        """
        self.bucket = bucket_name
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=aws_access_key_id,
            aws_secret_access_key=aws_secret_access_key,
            region_name=region
        )
    
    async def save(self, file_data: bytes, path: str) -> str:
        """保存文件到 S3
        
        Args:
            file_data: 文件二进制数据
            path: 文件路径（S3 Key）
            
        Returns:
            str: 可访问的文件URL
        """
        try:
            self.s3_client.put_object(
                Bucket=self.bucket,
                Key=path,
                Body=file_data
            )
            # 返回 S3 URL
            if settings.S3_URL:
                return f"{settings.S3_URL.rstrip('/')}/{path}"
            else:
                return f"https://{self.bucket}.s3.{self.s3_client.meta.region_name}.amazonaws.com/{path}"
        except ClientError as e:
            raise Exception(f"Failed to upload to S3: {str(e)}")
            
    async def delete(self, path: str) -> bool:
        """从 S3 删除文件
        
        Args:
            path: 文件路径（S3 Key）
            
        Returns:
            bool: 是否删除成功
        """
        try:
            self.s3_client.delete_object(
                Bucket=self.bucket,
                Key=path
            )
            return True
        except ClientError:
            return False

