# dn42peering Agent

# Maintainer: YuutaW <i@yuuta.moe>
pkgname=agent
pkgver=r1.a5ed3da
pkgrel=1
pkgdesc="dn42peering Agent"
arch=(any)
url="https://github.com/Trumeet/dn42peering"
license=('custom')
groups=()
depends=("systemd" "wireguard-tools" "bird")
makedepends=('java-environment>=8')
backup=("etc/dn42peering/agent.json")
options=()
source=("agent.json"
"agent.service")
noextract=()
md5sums=('e832e2216ebf4fc3b10bf5429c41c131'
         'afa1643528d4d2b65785714d32cba97f')

pkgver() {
	cd "$srcdir/.."
	printf "r%s.%s" "$(git rev-list --count HEAD)" "$(git rev-parse --short HEAD)"
}

build() {
	cd "$srcdir/.."
	./gradlew :agent:installDist
	sed -i 's/`pwd -P`/\/usr\/share\/java\/agent\//g' agent/build/install/agent/bin/agent
	sed -i 's/$APP_HOME\/lib/$APP_HOME/g' agent/build/install/agent/bin/agent
}

package() {
	cd "$srcdir/.."
	mkdir -p "$pkgdir/etc/dn42peering/"
	install -Dm644 $srcdir/agent.json "$pkgdir/etc/dn42peering/"
	mkdir -p "$pkgdir/usr/lib/systemd/system/"
	install -Dm644 $srcdir/agent.service "$pkgdir/usr/lib/systemd/system/"
	mkdir -p "$pkgdir/usr/share/java/agent/"
	cp -r agent/build/install/agent/lib/* "$pkgdir/usr/share/java/agent/"
	mkdir -p "$pkgdir/usr/bin/"
	cp -r agent/build/install/agent/bin/agent "$pkgdir/usr/bin/"
}
