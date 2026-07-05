# Infrastructure & Deployment Guide

This document outlines the deployment workflow and infrastructure setup for the Dynamic Rent Adjustment System (DRAS).

## Prerequisites
To run this application, ensure you have the following installed (if you are not using Vagrant):
- **Docker** and **Docker Compose**
- **Git**
- **Java 21+**, **Node.js 20+**, and **Maven** (for local development)

## Docker Deployment
The application is containerized using Docker and orchestrated with Docker Compose. Nginx acts as a reverse proxy,
routing traffic to the frontend and backend containers.

### Development Environment
To run the containers in a development setup:
```bash
docker compose up -d --build
```
Access the application at `http://localhost:5173` (or `http://localhost` if using the Nginx reverse proxy).

### Production Environment
The production configuration (`docker-compose.prod.yml`) includes restart policies, health checks,
named volumes, and log rotation. Nginx is the only publicly exposed service.

```bash
cp .env.example .env
# Edit .env with your credentials
docker compose -f docker-compose.prod.yml up -d --build
```

## Vagrant Installation
A complete, reproducible infrastructure is provided via Vagrant.
The `Vagrantfile` and `bootstrap.sh` automate the setup of an Ubuntu VM containing:
- Java 21
- Node.js 20
- Docker & Docker Compose
- Jenkins

To start the VM and trigger provisioning:
```bash
cd infrastructure/vagrant
vagrant up
```
The application will be accessible at `http://localhost:8080` (forwarded to the VM's port 80).
Jenkins will be accessible at `http://localhost:8888` on your host machine.

## Jenkins Setup
1. After starting Vagrant, open your browser and go to `http://localhost:8888`.
2. Retrieve the initial admin password by running:
   ```bash
   vagrant ssh -c "sudo cat /var/lib/jenkins/secrets/initialAdminPassword"
   ```
3. Install the suggested plugins and create your admin user.
4. Create a new "Pipeline" project.
5. Under the Pipeline section, set "Definition" to "Pipeline script from SCM" and point it to your Git repository URL.
6. Set the Script Path to `Jenkinsfile`.

## Deployment Workflow
The project includes a `Jenkinsfile` for CI/CD, containing stages for:
1. **Backend Build** (`mvn clean package -DskipTests`)
2. **Backend Tests** (`mvn test`)
3. **Frontend Install** (`npm install`)
4. **Frontend Build** (`npm run build`)
5. **Docker Image Build** (`docker compose -f docker-compose.prod.yml build`)
6. **Docker Compose Deployment** (`docker compose -f docker-compose.prod.yml up -d`)
7. **Health Check** (Verify Spring Boot Actuator)
8. **Archive Logs**

To deploy, simply click **Build Now** in your Jenkins pipeline.

## Troubleshooting

### General Issues
- **Database Connection Issues**: Ensure your `.env` credentials match the Spring Boot configuration.
  If using Docker, ensure the `db` service is healthy before `backend` starts.
- **Port Conflicts**: If port 8080 or 5432 is already in use on your host machine, modify `docker-compose.yml`
  or the `Vagrantfile` port forwarding settings.
- **Nginx 502 Bad Gateway**: Check the backend logs (`docker compose logs backend`).
  This typically occurs if the Spring Boot application fails to start or is still initializing.

### VirtualBox fails to start with Secure Boot enabled
If `vagrant up` reports that no provider is available, or loading the VirtualBox kernel module fails with:

`modprobe: ERROR: could not insert 'vboxdrv': Key was rejected by service`

Check whether the VirtualBox module signing key has been enrolled:

```bash
sudo mokutil --test-key /var/lib/shim-signed/mok/MOK.der
```

If the output is:

`/var/lib/shim-signed/mok/MOK.der is not enrolled`

Enroll the key:

```bash
sudo mokutil --import /var/lib/shim-signed/mok/MOK.der
```

Choose a temporary password when prompted, then reboot.

During the next boot, the MOK Manager screen will appear:

1. Select **Enroll MOK**
2. Select **Continue**
3. Select **Yes**
4. Enter the password you created
5. **Reboot**

Verify the enrollment:

```bash
sudo mokutil --test-key /var/lib/shim-signed/mok/MOK.der
```

The expected output is:

`/var/lib/shim-signed/mok/MOK.der is already enrolled`

Load the VirtualBox module:

```bash
sudo modprobe vboxdrv
```

Finally, start the VM:

```bash
vagrant up
```
