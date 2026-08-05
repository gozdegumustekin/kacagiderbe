# Kaça Gider — Backend Kurulum Kılavuzu

Bu doküman, **Kaça Gider** projesinin Java Spring Boot tabanlı backend servisinin yerel
geliştirme ortamında ve üretim (Railway) ortamında nasıl kurulup çalıştırılacağını anlatır.

Backend; kullanıcı kimlik doğrulama (JWT), e-posta doğrulama (Brevo), emlak fiyat tahmini
(Weka Random Forest) ve tahmin kayıtlarının saklanması (PostgreSQL) görevlerini üstlenir.
Görüntü doğrulama ve görsel kalite analizi ayrı bir **FastAPI / ML servisi** tarafından
yapılır; backend yalnızca doğrulanmış sonucu saklar.

---

## 1. Gereksinimler

Backend'i çalıştırmak için aşağıdaki araçların kurulu olması gerekir:

| Araç | Sürüm | Not |
|------|-------|-----|
| Java JDK | **21** | `pom.xml` içinde `java.version=21` olarak sabitlenmiştir. |
| Maven | 3.9+ | Projeyle birlikte gelen `mvnw` (Maven Wrapper) kullanılırsa ayrıca kurmaya gerek yoktur. |
| PostgreSQL | 14+ | Yerel ya da bulut bir PostgreSQL veritabanı. |
| Git | — | Depoyu klonlamak için. |

Java sürümünü doğrulamak için:

```bash
java -version
```

Çıktıda `21` görmelisiniz. Farklı bir sürüm görünüyorsa JDK 21 kurup `JAVA_HOME`
değişkenini ona yönlendirin.

---

## 2. Projeyi Edinme

```bash
git clone <depo-adresi> kacagider
cd kacagider
```

> **Not:** `application.properties` dosyası `.gitignore` içinde olduğundan depoda
> bulunmayabilir. Bulunmuyorsa [Bölüm 4](#4-yapılandırma-applicationproperties)'teki
> şablonu `src/main/resources/application.properties` olarak oluşturun.

---

## 3. Veritabanı Hazırlığı

Backend, başlangıçta tabloları otomatik oluşturur/günceller
(`spring.jpa.hibernate.ddl-auto=update`), bu yüzden tabloları elle oluşturmanıza
gerek yoktur. Yalnızca boş bir veritabanı oluşturmanız yeterlidir.

Yerel PostgreSQL'de örnek:

```sql
CREATE DATABASE kacagider;
```

Bağlantı bilgilerini bir sonraki bölümdeki ortam değişkenlerinde belirteceksiniz.

---

## 4. Yapılandırma (application.properties)

Tüm yapılandırma `src/main/resources/application.properties` üzerinden, **ortam
değişkenleriyle** yapılır. Değişken tanımlı değilse iki nokta üst üsteden sonraki
varsayılan değer kullanılır (`${DEGISKEN:varsayilan}`).

Aşağıda mevcut `application.properties` içeriği ve her ayarın açıklaması yer alır:

```properties
# === SUNUCU ===
server.port=${PORT:8080}
server.address=0.0.0.0
server.forward-headers-strategy=framework

# === VERİTABANI ===
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# === MAIL / BREVO API ===
app.mail.from=${APP_MAIL_FROM}
app.mail.fromName=${APP_MAIL_FROM_NAME:Kaça Gider}
brevo.api.key=${BREVO_API_KEY}

# === JWT ===
app.jwt.secret=${APP_JWT_SECRET}
app.jwt.accessTokenMinutes=${APP_JWT_ACCESSTOKENMINUTES:60}

# === E-POSTA DOĞRULAMA ===
app.frontend.verifyUrl=${APP_FRONTEND_VERIFY_URL:http://localhost:5173/verify-email?token=}
app.verification.tokenMinutes=${APP_VERIFICATION_TOKEN_MINUTES:60}

# === DOSYA YÜKLEME ===
app.upload.root=${APP_UPLOAD_ROOT:uploads}
app.upload.public-prefix=${APP_UPLOAD_PUBLIC_PREFIX:/uploads}
app.public-base-url=${APP_PUBLIC_BASE_URL:http://localhost:8080}

# === MODEL ===
model.enabled=${MODEL_ENABLED:true}
model.arff.path=${MODEL_ARFF_PATH:train_emlak_hepsi_1.arff}
model.file.path=${MODEL_FILE_PATH:emlak_rf_modeli.model.zip}

# === SWAGGER ===
springdoc.swagger-ui.path=/swagger
```

### 4.1. Zorunlu Ortam Değişkenleri

Aşağıdaki değişkenlerin **mutlaka** tanımlı olması gerekir (varsayılanları yoktur):

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC bağlantı adresi. `jdbc:` ile başlamalıdır. | `jdbc:postgresql://localhost:5432/kacagider` |
| `SPRING_DATASOURCE_USERNAME` | Veritabanı kullanıcı adı | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Veritabanı parolası | `gizliparola` |
| `APP_JWT_SECRET` | JWT imzalama anahtarı. Uzun ve rastgele olmalıdır (en az 32 karakter önerilir). | `b8f3...` (rastgele uzun dize) |
| `APP_MAIL_FROM` | E-postaların gönderileceği adres (Brevo'da doğrulanmış olmalı) | `noreply@kacagider.com` |
| `BREVO_API_KEY` | Brevo (Sendinblue) hesabından alınan API anahtarı | `xkeysib-...` |

### 4.2. İsteğe Bağlı Ortam Değişkenleri

Bunların makul varsayılanları vardır; yalnızca değiştirmek isterseniz tanımlayın:

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| `PORT` | `8080` | Sunucunun dinleyeceği port |
| `APP_MAIL_FROM_NAME` | `Kaça Gider` | E-postalarda görünen gönderen adı |
| `APP_JWT_ACCESSTOKENMINUTES` | `60` | Access token geçerlilik süresi (dakika) |
| `APP_FRONTEND_VERIFY_URL` | `http://localhost:5173/verify-email?token=` | E-posta doğrulama bağlantısının frontend adresi |
| `APP_VERIFICATION_TOKEN_MINUTES` | `60` | Doğrulama kodunun geçerlilik süresi (dakika) |
| `APP_UPLOAD_ROOT` | `uploads` | Yüklenen dosyaların kaydedileceği kök klasör |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8080` | Yüklenen dosyalara erişim için temel URL |
| `MODEL_ENABLED` | `true` | `false` yapılırsa fiyat tahmini endpoint'i kapanır, diğer endpoint'ler çalışmaya devam eder |

> **MODEL_ENABLED ipucu:** Modeller henüz hazır değilken backend'i yine de ayağa
> kaldırmak isterseniz `MODEL_ENABLED=false` yapın. Modeller hazır olduğunda
> `true` yapıp yeniden başlatın.

### 4.3. Ortam Değişkenlerini Tanımlama

**Yerel geliştirmede (Linux/macOS):**

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/kacagider"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="gizliparola"
export APP_JWT_SECRET="cok-uzun-rastgele-bir-anahtar-buraya"
export APP_MAIL_FROM="noreply@kacagider.com"
export BREVO_API_KEY="xkeysib-..."
```

**Yerel geliştirmede (Windows PowerShell):**

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/kacagider"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="gizliparola"
$env:APP_JWT_SECRET="cok-uzun-rastgele-bir-anahtar-buraya"
$env:APP_MAIL_FROM="noreply@kacagider.com"
$env:BREVO_API_KEY="xkeysib-..."
```

IDE (IntelliJ IDEA / VS Code) kullanıyorsanız, çalıştırma yapılandırmasının
(Run Configuration) "Environment variables" bölümüne de aynı değişkenleri
girebilirsiniz.

---

## 5. Çalıştırma

### 5.1. Maven Wrapper ile (önerilen)

Projeyle birlikte gelen `mvnw` betiği, doğru Maven sürümünü kendi indirir; ayrıca
Maven kurmanıza gerek kalmaz.

**Linux/macOS:**

```bash
./mvnw spring-boot:run
```

**Windows:**

```cmd
mvnw.cmd spring-boot:run
```

İlk çalıştırmada Maven, bağımlılıkları (Spring Boot, Weka, JWT, PostgreSQL sürücüsü
vb.) internetten indireceği için biraz zaman alabilir.

### 5.2. Sistemde kurulu Maven ile

Maven kuruluysa wrapper yerine doğrudan da çalıştırabilirsiniz:

```bash
mvn spring-boot:run
```

### 5.3. Başarılı başlangıç

Uygulama açılırken, modellerin yüklendiğini bildiren log satırlarını görmelisiniz.
On Weka modeli (`tek`, `kume3` grupları, `kademe1–6`) sıkıştırılmış `.zip` halinden
açılıp belleğe yüklenir ve aktif stratejinin (`kume3`) hazır olduğu bildirilir.
Son olarak şuna benzer bir satır görürsünüz:

```
Started KacagiderApplication in X.X seconds
```

---

## 6. Doğrulama ve Test

Uygulama ayağa kalktıktan sonra:

- **Swagger arayüzü:** Tarayıcıdan `http://localhost:8080/swagger` adresine giderek
  tüm uç noktaları görüp deneyebilirsiniz.
- **Sağlık kontrolü:** Kayıt veya giriş uç noktalarını Swagger üzerinden test edin.

### Başlıca uç noktalar

| Yöntem | Yol | Açıklama |
|--------|-----|----------|
| POST | `/api/auth/register` | Yeni kullanıcı kaydı |
| POST | `/api/auth/login` | Giriş, JWT döner |
| POST | `/api/auth/verify-email` | E-posta doğrulama |
| POST | `/api/auth/resend-code` | Doğrulama kodunu yeniden gönder |
| GET  | `/api/prediction/form-data` | Form için il/ilçe vb. meta verileri |
| POST | `/api/prediction` | Fiyat tahmini (aralık) |
| POST | `/api/predictions` | Tahmin kaydı oluştur |
| GET  | `/api/predictions/my` | Kullanıcının geçmiş tahminleri |
| GET  | `/api/predictions/{id}` | Tek bir tahmin kaydı |
| POST | `/api/predictions/{id}/images` | Tahmine ait doğrulanmış görselleri ekle |
| POST | `/api/predictions/{id}/recompute` | Görsel skorlarıyla yeniden hesapla |

`/api/prediction` çağrısında, aktif modelleme stratejisini sorgu parametresiyle
geçici olarak değiştirebilirsiniz (ör. `?strateji=tek` veya `?strateji=kademe6`).
Parametre verilmezse `pipeline_config.json` içindeki aktif strateji (`kume3`)
kullanılır.

---

## 7. Üretime Alma (Railway)

Üretim ortamı **Railway** üzerinde, doğrudan GitHub deposundan derlenerek çalışır.
Yerel makineden dağıtım yapılmaz; Railway depoyu kendisi çeker ve derler.

Railway'de dikkat edilecekler:

1. **Root Directory:** Servis ayarlarında kök dizin **`kacagider`** olmalıdır
   (projenin `pom.xml` dosyasının bulunduğu klasör). Yanlış kök dizin, derlemenin
   ve model dosyalarının bulunamamasının en yaygın nedenidir.
2. **Ortam değişkenleri:** [Bölüm 4.1](#41-zorunlu-ortam-değişkenleri)'deki tüm
   zorunlu değişkenler Railway'in "Variables" sekmesine girilmelidir.
   `SPRING_DATASOURCE_URL` JDBC biçiminde (`jdbc:postgresql://...`) olmalıdır.
3. **PostgreSQL:** Railway'in yönetilen PostgreSQL eklentisi kullanılabilir;
   bağlantı bilgileri ortam değişkenlerine işlenir.
4. **PORT:** Railway portu otomatik atar; uygulama `${PORT}` değişkenini okuduğu
   için ek bir ayar gerekmez.

Railway'de ortam değişkenleri, `application.properties` içindeki değerleri ezdiği
için yapılandırmayı yeniden dağıtım yapmadan değiştirebilirsiniz.

---

## 8. Modeller Hakkında Notlar

- Backend, on adet Weka Random Forest modelini `src/main/resources/` altında
  **sıkıştırılmış** (`.zip`) olarak taşır ve açılışta belleğe açar. Weka modelleri
  çok tekrarlı olduğundan zip ile yaklaşık %97 oranında küçülür; bu sayede dosya
  boyutu sorunları yaşanmadan depoya eklenebilirler.
- Model dosyalarının adları `pipeline_config.json` içindeki tanımlarla birebir
  eşleşmelidir (ör. config'te `model_tek.model` için dosya `model_tek.model.zip`
  olmalıdır).
- İl → SEGE kademesi eşlemesi `sege_kademe.json` üzerinden çalışma anında yapılır.
  İl adı karşılaştırması büyük/küçük harften bağımsızdır (Türkçe-duyarlı normalize
  edilir), bu nedenle frontend'in gönderdiği `ANKARA` ile config'teki `Ankara`
  sorunsuz eşleşir.

---

## 9. Sık Karşılaşılan Sorunlar

| Belirti | Olası neden / çözüm |
|---------|---------------------|
| `SPRING_DATASOURCE_URL ... must start with "jdbc"` | Veritabanı adresi `jdbc:postgresql://` ile başlamıyor. URL biçimini düzeltin. |
| Açılışta veritabanı bağlantı hatası | PostgreSQL çalışmıyor ya da kullanıcı adı/parola/port yanlış. Bağlantı bilgilerini doğrulayın. |
| `APP_JWT_SECRET` ile ilgili hata / token üretilemiyor | `APP_JWT_SECRET` tanımlı değil ya da çok kısa. Uzun, rastgele bir değer atayın. |
| Model dosyası bulunamadı | (Railway'de) Root Directory `kacagider` değil; ya da model `.zip` adı config ile uyuşmuyor. |
| E-posta gönderilemiyor | `BREVO_API_KEY` geçersiz ya da `APP_MAIL_FROM` Brevo'da doğrulanmamış. |
| Java sürüm hatası (`release 21 not supported`) | JDK 21 kurulu değil. JDK 21 kurup `JAVA_HOME`'u güncelleyin. |
| Fiyat tahmini endpoint'i 404/kapalı | `MODEL_ENABLED=false` olabilir. `true` yapıp yeniden başlatın. |

---

## 10. Özet — Hızlı Başlangıç

```bash
# 1. Depoyu klonla
git clone <depo-adresi> kacagider && cd kacagider

# 2. PostgreSQL'de boş veritabanı oluştur
#    (psql veya pgAdmin ile: CREATE DATABASE kacagider;)

# 3. Zorunlu ortam değişkenlerini tanımla
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/kacagider"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="gizliparola"
export APP_JWT_SECRET="cok-uzun-rastgele-bir-anahtar"
export APP_MAIL_FROM="noreply@kacagider.com"
export BREVO_API_KEY="xkeysib-..."

# 4. Çalıştır
./mvnw spring-boot:run

# 5. Tarayıcıda aç
#    http://localhost:8080/swagger
```

Backend bu adımların ardından çalışır durumda olur. Görüntü doğrulama ve görsel
kalite analizi için ayrı **ML servisinin** (FastAPI) de çalışıyor olması gerekir;
onun kurulumu kendi kılavuzunda anlatılmaktadır.
