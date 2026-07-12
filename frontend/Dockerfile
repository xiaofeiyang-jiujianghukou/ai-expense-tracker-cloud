FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/node:20-alpine

WORKDIR /app

# npm registry mirror (aliyun)
RUN npm config set registry https://registry.npmmirror.com

# Install dependencies first (cache layer)
COPY package.json package-lock.json* ./
RUN npm install

# Copy source
COPY . .

EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
