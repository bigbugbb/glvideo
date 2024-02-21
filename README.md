<p align="center">
  <h2 align="center">GLVideo</h3>

  <p align="center">
    一个基于图连接、Kotlin协程和GLES的简单音视频框架
    <br/>
    <br/>
    <a href="https://github.com/bigbugbb/glvideo/issues">Report Bug</a>
    .
    <a href="https://github.com/bigbugbb/glvideo/issues">Request Feature</a>
  </p>
</p>

![Contributors](https://img.shields.io/github/contributors/bigbugbb/glvideo?color=dark-green) ![Issues](https://img.shields.io/github/issues/bigbugbb/glvideo) ![License](https://img.shields.io/github/license/bigbugbb/glvideo)

## About The Project

<p style="font-size: 16px;">
开发公司App过程中，设计提出了渲染3d卡片并分享渲染视频的需求。iOS端用SceneKit可以实现，Android这边没有相应工具链，我

只能DIY一套工具自己实现:

* 为实现渲染，我加入了OpenGLES渲染操作相关工具，包括纹理操作，基础shader, FBO操作, 基础Renderer类和定制化GLSurfaceView，定制化RenderThread等。同时把每个渲染元素抽象成一个Drawer，分开管理不同元素的顶点坐标、纹理坐标、坐标变换、坐标数据加载、viewport和具体绘制逻辑。
* 为实现录制，加入了自己的MediaCodec硬编码操作和编码器端的Renderer。该Renderer主要为了解耦encoder和待编码纹理源，因为源纹理尺寸可以和encoder编码尺寸不一致，同时这也赋予了encoder对纹理图像进行后处理的灵活度。
* 开发过程中为在不同线程共享GL资源，参考了字节流动的技术文章实现了灵活的EGL Context管理（主要涉及EGL资源创建，共享EGL资源到具体线程的绑定和解绑定）。
* 卡片上还需要显示一个视频，因此又加入了基于MediaCodec的硬解码和帧缓冲区位块传送（Blit）。

[![3d card]](https://github.com/bigbugbb/glvideo/assets/5157712/f2e332e8-b4ce-4806-90b7-63b64bf2d8d3)

基于这套技术工具，后来又实现了给视频加水印，加开头和结尾内容，基于CameraX的视频帧采集、渲染和简单特效。

公司App内可以进入语音房，在语音房里的speaker可以开视频，房主可以发起房间快照转视频并分享的功能。具体来说，首先需要同步房员状态（包括将座位UI动态改变成九宫格，界面上显示倒计时等），接着需要采集房间成员在倒计时结束后一秒内的所有视频帧（240*240的尺寸，最多同时收集9个源，framerate不大于24）。
由于源数据帧来自不同线程，为了在采集时实现纹理共享，又实现了简单的纹理池（纹理池自己维护了一个GL线程，通过将纹理池的EGLContext分享给数据采集线程，实现纹理共享）。为了将采集的ByteBuffer数据上传到具体的纹理缓冲，又加入了PBO操作进行性能优化。

App临死前还加入了设置用户webp头像的功能，这里先通过摄像头录制视频或加载本地视频，接着实现裁剪工具进行裁剪，获得一个mp4文件，再通过ffmpeg转码，生成webp。

[![video cut]](https://github.com/bigbugbb/glvideo/assets/5157712/14f5bc9c-e5e2-4534-abaa-50fdb33230bb)

以上开发过程中，基于对现有代码复用、组件交互简化、线程操作简化、降低学习成本等考虑，实现了一套基于图和协程的Kotlin端简易视频处理框架，这里打包成库和SampleApp，方便以后学习交流。
</p>

## Prerequisites

* Android 7.1+

## Roadmap

<p style="font-size: 16px;">
未来短期目标是逐步加入native部分，用native代码取代上层代码，在native部分直接整合ffmpeg的使用，通过ffmpeg进行硬解和硬编，简化上层逻辑。
更远点的目标是移植到其他开发环境。
</p>

## License

Distributed under the MIT License. See [LICENSE](https://github.com/bigbugbb/glvideo/blob/master/LICENSE.md) for more information.

## Authors

* **Bin Bo** - *QVOD player builder，Lobby App builder，GLVideo builder*
