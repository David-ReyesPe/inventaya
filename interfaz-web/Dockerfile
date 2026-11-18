# Interfaz estatica: no requiere paso de build ni node_modules, solo nginx sirviendo los
# archivos y haciendo de reverse proxy hacia los dos servicios.
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY proxy-comun.inc /etc/nginx/conf.d/proxy-comun.inc
COPY html/ /usr/share/nginx/html/

EXPOSE 80
