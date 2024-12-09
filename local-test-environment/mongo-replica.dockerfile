FROM mongo:4.0@sha256:4ca81c89ad08f4cfa9906005126112bffe8fb363800466ef5e50f6238f6f6af1
COPY ./entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

ENTRYPOINT [ "sh", "/entrypoint.sh" ]