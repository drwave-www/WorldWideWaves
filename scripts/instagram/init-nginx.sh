#!/bin/sh

# Generate the .htpasswd file
if [ -n "$NGINX_USERNAME" ] && [ -n "$NGINX_PASSWORD" ]; then
    echo "Generating .htpasswd for user: $NGINX_USERNAME"
    apk add --no-cache apache2-utils > /dev/null 2>&1
    htpasswd -bc /etc/nginx/.htpasswd "$NGINX_USERNAME" "$NGINX_PASSWORD"
else
    echo "NGINX_USERNAME or NGINX_PASSWORD not set, skipping .htpasswd generation."
fi

# Start NGINX
exec "$@"

