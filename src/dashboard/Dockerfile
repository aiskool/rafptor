FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN addgroup -g 10003 -S rafptor && adduser -u 10003 -S rafptor -G rafptor
USER rafptor
EXPOSE 8080
