# minio-spring-demo
MinIO S3 spring demo

Run MinIO docker
```
docker rm -f minio-local; docker run -p 9000:9000 -p 9001:9001 -e "MINIO_ROOT_USER=admin" -e "MINIO_ROOT_PASSWORD=adminadmin" --name minio-local -d quay.io/minio/minio server /data --console-address ":9001"
```
