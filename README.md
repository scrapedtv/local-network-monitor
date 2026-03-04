<img src="https://capsule-render.vercel.app/api?type=waving&color=0:000000,40:0a1a0a,80:0d2b0d,100:000000&height=220&section=header&text=NetMonitor&fontSize=80&fontColor=4ade80&fontAlignY=52&animation=fadeIn&stroke=4ade80&strokeWidth=2&desc=Local%20Network%20Monitor%20%20.%20%20Java%2021%20%20.%20%20NIO%20%2B%20Concurrency%20%20.%20%20Dark%20GUI&descSize=15&descAlignY=75&descColor=4b7c4b" width="100%"/>

<div align="center">

<br/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=JetBrains+Mono&weight=700&size=18&pause=900&color=4ADE80&center=true&vCenter=true&width=750&lines=Local+Network+Monitor+%E2%80%94+pure+Java+21+%F0%9F%96%A5%EF%B8%8F;Device+Discovery+%7C+Port+Scanner+%7C+Ping+Graph;NIO+SocketChannels+%2B+ExecutorService+%2B+Concurrency;No+dependencies.+Just+Java.+%F0%9F%9A%80;Open+source.+Dark+GUI.+Real+results.)](https://git.io/typing-svg)

<br/>

![Java](https://img.shields.io/badge/Java-21-4ade80?style=for-the-badge&logo=openjdk&logoColor=4ade80&labelColor=0a0f0a)
&nbsp;
![Gradle](https://img.shields.io/badge/Gradle-8.7-4ade80?style=for-the-badge&logo=gradle&logoColor=4ade80&labelColor=0a0f0a)
&nbsp;
![License](https://img.shields.io/badge/License-MIT-4ade80?style=for-the-badge&logoColor=4ade80&labelColor=0a0f0a)
&nbsp;
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Mac%20%7C%20Linux-4ade80?style=for-the-badge&logoColor=4ade80&labelColor=0a0f0a)

</div>

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ was ist das }`

**NetMonitor** ist ein lokales Netzwerk-Monitoring-Tool mit einer professionellen Dark-GUI — gebaut in reinem **Java 21**, ohne externe Dependencies. Kein Wireshark, kein nmap, kein Setup-Aufwand. Einfach starten und sehen, was im Netzwerk passiert.

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ features }`

<table>
<tr>
<td width="50%" valign="top">

**🖥️ Dashboard**
- Live-Übersicht aller aktiven Netzwerkinterfaces
- IPv4, MAC, MTU, Interface-Typ auf einen Blick
- Automatische Erkennung der lokalen IP

**📡 Device Discovery**
- ICMP + TCP-Fallback Subnet-Sweep (.1 – .254)
- Concurrent via `ExecutorService` mit 128 Worker-Threads
- Hostname-Auflösung, RTT-Messung, Erkennungsmethode

</td>
<td width="50%" valign="top">

**🔌 Port Scanner**
- NIO `SocketChannel` + `Selector` für non-blocking probes
- 35+ bekannte Services im eingebautem Registry
- Custom Range oder Preset "Common Ports"

**📶 Ping Monitor**
- Continuous Ping mit Live-Graph (letzten 60 Werte)
- Min / Max / Avg / Loss Statistiken in Echtzeit
- Timeout-Markierung im Graphen

**📋 Traffic Log**
- Ring-Buffer (2000 Einträge) + persistente File-Ausgabe
- Alle Events aus allen Panels geloggt
- `~/.netmonitor/netmonitor.log`

</td>
</tr>
</table>

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ stack }`

<div align="center">

<img src="https://skillicons.dev/icons?i=java,idea,gradle,git,github&theme=dark&perline=5"/>

<br/><br/>

```
Java 21          →  Core Language, Records, Pattern Matching
Swing + Nimbus   →  Dark UI, Custom Renderer, Paint Overrides
NIO              →  SocketChannel, Selector (non-blocking Port Scan)
ExecutorService  →  ThreadPoolExecutor, 128 Threads, concurrent sweep
ScheduledExec    →  Ping Loop mit konfigurierbarem Interval
InetAddress      →  ICMP Reachability + Hostname Resolution
NetworkInterface →  Interface Enumeration, MAC, MTU
```

</div>

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ quickstart }`

**Voraussetzung:** Java 21 installiert → [adoptium.net](https://adoptium.net)

```bash
# Check
java -version   # muss 21+ sein

# Clone
git clone https://github.com/DEIN_USERNAME/local-network-monitor.git
cd local-network-monitor

# Run (Gradle lädt sich beim ersten Mal selbst)
./gradlew run          # Mac / Linux
gradlew.bat run        # Windows
```

> **IntelliJ:** Ordner öffnen → Gradle-Import bestätigen → `▶ Run`

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ struktur }`

```
local-network-monitor/
├── build.gradle                          Groovy DSL, fat jar, Java 21
├── settings.gradle
├── gradle/wrapper/
└── src/main/java/net/monitor/
    ├── Main.java                         Bootstrap + Nimbus Dark Theme
    ├── NetworkCore.java                  ThreadPoolExecutor, Interface-Snapshots
    ├── DeviceScanner.java                ICMP + TCP Concurrent Subnet Sweep
    ├── PortScanner.java                  NIO SocketChannel, 35+ Services
    ├── PingService.java                  Scheduled RTT, Stats, Loss
    ├── TrafficLogger.java                Ring-Buffer + File Persistence
    └── MonitorUI.java                    Dark Swing GUI, alle Panels, Ping Graph
```

7 Dateien. Keine externen Dependencies. Kein Overhead.

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ build }`

```bash
# Fat JAR bauen
./gradlew jar

# Starten
java -jar build/libs/netmonitor.jar
```

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

### `{ hinweis }`

Device Discovery und Port Scanner senden Netzwerkpakete. Nur in eigenen Netzwerken oder mit expliziter Erlaubnis verwenden. Das Tool ist für lokale Diagnose und Lernzwecke gebaut.

<br/>

<img src="https://capsule-render.vercel.app/api?type=rect&color=4ade80&height=2" width="100%"/>

<br/>

<div align="center">

**MIT License · gebaut von [ScrapedTV](https://github.com/scrpdtv)**

<br/>

<a href="mailto:scrpdtv@gmail.com">
<img src="https://img.shields.io/badge/scrpdtv%40gmail.com-kontakt-4ade80?style=for-the-badge&logo=gmail&logoColor=4ade80&labelColor=0a0f0a"/>
</a>
&nbsp;
<a href="https://discord.gg/meloncity">
<img src="https://img.shields.io/badge/MelonCity-discord.gg%2Fmeloncity-4ade80?style=for-the-badge&logo=discord&logoColor=4ade80&labelColor=0a0f0a"/>
</a>

</div>

<br/>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:000000,40:0a1a0a,80:0d2b0d,100:000000&height=140&section=footer" width="100%"/>