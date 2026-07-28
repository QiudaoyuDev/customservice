#!/usr/bin/env bash
set -Eeuo pipefail

# Installs Docker Engine and the Docker Compose plugin on CentOS Stream, Rocky
# Linux, AlmaLinux, RHEL or compatible RPM-based servers. Run once as root.

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root: sudo bash scripts/install-docker-centos.sh" >&2
  exit 1
fi

if command -v docker >/dev/null 2>&1; then
  echo "Docker is already installed: $(docker --version)"
  exit 0
fi

if command -v dnf >/dev/null 2>&1; then
  PKG=dnf
elif command -v yum >/dev/null 2>&1; then
  PKG=yum
else
  echo "No supported package manager found. This script requires dnf or yum." >&2
  exit 1
fi

${PKG} -y install dnf-plugins-core ca-certificates curl
${PKG} config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
${PKG} -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable --now docker
docker --version
docker compose version
echo "Docker is installed and enabled. Add non-root operators to the docker group only if your security policy allows it."
