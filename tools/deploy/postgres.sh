#!/bin/bash

DB=jm
DB_USER=${DB}
DB_PASS=${DB}

apt-get -y update
apt-get -y upgrade

# Solving locale warnings
locale-gen en_US.UTF-8
sudo dpkg-reconfigure locales

/etc/profile.d/locale.sh <<EOF
export LANGUAGE=en_US.UTF-8
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
EOF

apt-get -y install postgresql

useradd -m jm

sudo -i -u postgres psql <<EOF
CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASS}';
CREATE DATABASE ${DB};
\q
EOF

PG_CONF_DIR=/etc/postgresql/9.3/main
PG_CONF=${PG_CONF_DIR}/postgresql.conf
PG_HBA_CONF=${PG_CONF_DIR}/pg_hba.conf

sed -i.orig "s/#listen_addresses = 'localhost'/listen_addresses = '*'/" ${PG_CONF}

if [ ! -f ${PG_HBA_CONF}.orig ]; then
    cp ${PG_HBA_CONF} ${PG_HBA_CONF}.orig
fi

echo "host  jm  jm  0.0.0.0/0   password" >> ${PG_HBA_CONF}

service postgresql restart

