#include <esp32cam.h>
#include <WebServer.h>
#include <WiFi.h>
#include <Base64.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <type_traits>
                                                                                                                               
void sendPostRequest(String postData);
const char *WIFI_SSID = "iPhone"; // 改成自己的wifi名称
const char *WIFI_PASS = "12345678";    // 改成自己的wifi密码
const char *SERVER_BASE_URL = "http://172.20.10.3:8000/"; // 与手机设置页中的服务器地址保持一致
int isFind = 0;
int LED_State = 0, Key_end = 1;
int LED_Key = 0,Call_Key = 0,Photo_Key = 0;
//int close_led = 1;
WebServer server(80);

String base64Data;
String postData;
static auto loRes = esp32cam::Resolution::find(640, 640);

String serverValue;

unsigned long lastTick = 0;

void serveJpg() // 定义一个函数，用于服务JPEG格式的图像，用户在浏览器看到图片
{
  // 捕获一帧图像
  auto frame = esp32cam::capture(); // 调用 esp32cam 库的 capture 方法捕获一帧图像
  if (frame == nullptr)
  {                                 // 如果捕获的图像为空（即捕获失败）
    // Serial.println("CAPTURE FAIL"); // 在串行监视器上打印“CAPTURE FAIL”
    server.send(503, "", "");       // 向客户端发送 HTTP 503 错误响应（服务不可用）
    return;                         // 结束函数执行，不再继续后续操作
  }

  // 设置HTTP响应的内容长度，即JPEG图像的大小
  server.setContentLength(frame->size());

  // 发送HTTP 200 OK响应，并指定内容类型为 JPEG 图像
  server.send(200, "image/jpeg");

  // 获取当前与服务器连接的客户端对象
  WiFiClient client = server.client();

  // 将捕获的图像帧写入到客户端，即发送给请求图像的客户端
  frame->writeTo(client);
}

// handleJpgLo 函数：处理请求低分辨率JPEG图像的请求
void handleJpgLo()
{
  // 尝试将摄像头分辨率设置为低分辨率
  if (!esp32cam::Camera.changeResolution(loRes))
  {
    // Serial.println("设置低分辨率失败");
  }
  // 调用 serveJpg 函数捕获并发送JPEG图像
  serveJpg();
  // postJpg();
}

// 获取发送数据
void postJpg()   //发送图片到服务器，进行数据处理
{
  // 捕获一帧图像
  auto frame = esp32cam::capture(); // 调用 esp32cam 库的 capture 方法捕获一帧图像
  if (frame == nullptr)
  {
    // Serial.println("CAPTURE FAIL"); // 在串行监视器上打印“CAPTURE FAIL”
    server.send(503, "", "");       // 向客户端发送 HTTP 503 错误响应（服务不可用）
    return;                         // 结束函数执行，不再继续后续操作
  }

  // 将捕获的图像帧转换为Base64编码
  base64Data = base64::encode(frame->data(), frame->size());

  // 构建HTTP POST请求数据
  postData = "{\"image\":\"";
  postData += base64Data;
  postData += "\"}";

  // 发送HTTP POST请求到后端服务器
  sendPostRequest(postData);
}

// 语音模块播报
void speak(String result)
{
  result.trim();

  if (result.indexOf("STOP") != -1)
  {
    Serial.print(1);
  }
  else if (result.indexOf("PROCEED") != -1)
  {
    Serial.print(2);
  }
  else if (result.indexOf("CAUTION") != -1)
  {
    Serial.print(3);
  }
  else if (result.indexOf("no") != -1)
  {
    Serial.print(3);
  }
  else if (result.indexOf("red") != -1)
  {
    Serial.print(1);
  }
  else if (result.indexOf("green") != -1)
  {
    Serial.print(2);
  }
}

// 寻找导盲杖
void find(){
  server.send(200, "", ""); // 发送HTTP 200 OK响应
  isFind=!isFind;
  if(isFind) Serial.print(4);
  else Serial.print(5);
}

// 发送http请求至后端
void sendPostRequest(String postData)
{
  HTTPClient http;
  if (http.begin(String(SERVER_BASE_URL) + "Android/identify/"))
  {
    http.addHeader("Content-Type", "application/json");

    int httpResponseCode = http.POST(postData);

    if (httpResponseCode > 0)
    {
      String response = http.getString();
      speak(response);
    }
    http.end();
  }
}

// 紧急联系人发送http请求
void httpCall(){
  HTTPClient http;
  if (http.begin(String(SERVER_BASE_URL) + "Android/emergency/"))
  {
    http.addHeader("Content-Type", "application/json");
    int httpResponseCode = http.GET();
    http.end();
  }
}

//读取服务器中的值来判断是否触发寻找导盲杖
void fetchServerValue() {
  HTTPClient http;
  if (http.begin(String(SERVER_BASE_URL) + "Android/find_stick_status/")) {
    int httpCode = http.GET();

    if (httpCode == 200) {
      String payload = http.getString();
      // serverValue = payload.toInt();
      serverValue = payload;
      Serial.print(serverValue);
    }
    http.end();
  }
}

void open_LED()
{
  digitalWrite(4, HIGH);
  LED_State = 1;
}

void close_LED()
{
  digitalWrite(4, LOW);
  LED_State = 0;
}

void setup() 
{


  // 初始化串口通信，设置波特率为115200
   Serial.begin(115200);

  //使用esp32cam命名空间中的类和函数
  {
    using namespace esp32cam;
    // 创建一个配置对象
    Config cfg;
    // 设置摄像头引脚配置为AiThinker标准
    cfg.setPins(pins::AiThinker);
    // 设置摄像头分辨率为高分辨率
    cfg.setResolution(loRes);
    // 设置缓冲区数量为2，这可能会影响摄像头的性能
    cfg.setBufferCount(2);
    // 设置JPEG压缩质量为92（1-100的范围内）
    cfg.setJpeg(92);
    // 初始化摄像头，如果成功则打印"CAMERA OK"，否则打印"CAMERA FAIL"
    bool ok = Camera.begin(cfg);
  }

  // 禁用WiFi配置持久化
  WiFi.persistent(false);
  // 设置WiFi模式为STA（Station模式，即客户端模式）
  WiFi.mode(WIFI_STA);
  // 连接到指定的WiFi网络
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  //等待WiFi连接成功
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500); // 等待500毫秒后重试
  }

  //为Web服务器设置不同的处理函数
  server.on("/open_LED", open_LED);                     // 打开LED灯处理
  server.on("/close_LED", close_LED);                   // 关闭LED灯处理
  server.on("/isFind", find);                           // 寻找导盲杖
  // 开始Web服务器
  server.begin();

  pinMode(15, INPUT_PULLDOWN); // 红绿灯识别按钮
  pinMode(14, INPUT_PULLDOWN); // 紧急呼救按钮
  pinMode(2, INPUT_PULLDOWN);  // 夜间LED按钮

  pinMode(4, OUTPUT);
  digitalWrite(4, LOW);

  delay(500); //启动缓冲
}

void loop() 
{
  
    //Serial.println("Hello");

    LED_Key = digitalRead(2);    //夜间LED
    Call_Key =digitalRead(14);   //紧急联系人
    Photo_Key = digitalRead(15); //拍照上传

  //LED按键检测
    if(LED_Key == 1 && Key_end == 1)
    {
      Key_end = 0;
      delay(10);        //消抖
      LED_Key = digitalRead(2);
      if(LED_Key == 1)
      {
        if(LED_State == 1)
        {
          close_LED();
        }
        else
        {
          open_LED();
        }
      }
    } 
    else if(LED_Key == 0)
    {
      Key_end = 1;
    }

  //紧急联系人按键检测
    if(Call_Key == 1)
    {
      delay(10);
      Call_Key = digitalRead(14);
      if(Call_Key == 1)
      {
        httpCall();
      }
    }

  //拍照按键检测
    if(Photo_Key == 1)
    {
      delay(10);
      Photo_Key = digitalRead(15);
      if(Photo_Key == 1)
      {
        postJpg();
      }
    }

    //每隔1秒检查一下服务器中的值
    unsigned long now = millis();
    if(now - lastTick >= 1000)
    {
      lastTick = now;
      fetchServerValue();
      //Serial.print(9);
    }
    if(serverValue == "true")
    {
      Serial.print(4);
      delay(300);
    }

   // 处理Web服务器的客户端请求
    server.handleClient();
    delay(20);
}
