#!/bin/bash
set -e

# Add DEBIAN_FRONTEND=noninteractive to avoid interactive prompts during provisioning
export DEBIAN_FRONTEND=noninteractive

echo "Updating system..."
apt-get update -y

echo "Installing prerequisites..."
apt-get install -y ca-certificates curl gnupg lsb-release wget apt-transport-https git

# Create keyrings directory if it doesn't exist
install -m 0755 -d /etc/apt/keyrings

# Docker & Docker Compose Plugin
echo "Installing Docker..."
if ! command -v docker >/dev/null 2>&1; then
    # Install Docker from official repository
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | tee /etc/apt/keyrings/docker.asc > /dev/null
    chmod a+r /etc/apt/keyrings/docker.asc

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null
      
    apt-get update -y
    # Install official Docker packages and the docker-compose-plugin
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    systemctl enable docker
    systemctl start docker
else
    echo "Docker is already installed, skipping."
fi

# Make vagrant user a member of the docker group
if ! getent group docker | grep -q "\bvagrant\b"; then
    usermod -aG docker vagrant
fi

# Jenkins
echo "Installing Jenkins..."
if ! command -v jenkins >/dev/null 2>&1; then
    curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | tee /etc/apt/keyrings/jenkins-keyring.asc > /dev/null
    echo deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | tee /etc/apt/sources.list.d/jenkins.list > /dev/null
    
    apt-get update -y
    apt-get install -y jenkins
    systemctl enable jenkins
    systemctl start jenkins
else
    echo "Jenkins is already installed, skipping."
fi

# Make jenkins user a member of the docker group
if ! getent group docker | grep -q "\bjenkins\b"; then
    usermod -aG docker jenkins
    systemctl restart jenkins
fi

echo "Provisioning complete!"
