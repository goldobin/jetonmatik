#!/bin/bash

DB=jm
DB_USER=${DB}
DB_PASS=${DB}

apt-get -y update
apt-get -y upgrade

mkdir -p /etc/mysql/conf.d

/bin/cat <<EOM >/etc/mysql/conf.d/mysqld_listen_all.cnf
[mysqld]
bind-address = 0.0.0.0
EOM

export DEBIAN_FRONTEND=noninteractive
apt-get -y install mariadb-server

mysql -u root -e "create database $DB; grant all privileges on $DB.* to '$DB_USER'@'%' identified by '$DB_PASS';"

