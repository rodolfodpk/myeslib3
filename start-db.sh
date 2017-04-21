docker run --name postgresql -itd --restart always \
  --env 'PG_TRUST_LOCALNET=true' \
  --env 'PG_PASSWORD=password' \
  --env 'DB_USER=dbuser' --env 'DB_PASS=dbuserpass' \
  --env 'DB_NAME=dbname1' \
  --publish 5432:5432 \
  --volume /srv/docker/postgresql:/var/lib/postgresql \
  sameersbn/postgresql:9.6-2
