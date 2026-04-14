interface AudioPlayerProps {
  src: string;
}

export default function AudioPlayer({ src }: AudioPlayerProps) {
  return (
    <div
      style={{
        padding: 12,
        background: '#f0f5ff',
        borderRadius: 8,
        marginTop: 8,
      }}
    >
      <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8, color: '#6366f1' }}>
        AI 播客音频
      </div>
      <audio controls src={src} style={{ width: '100%' }}>
        您的浏览器不支持音频播放
      </audio>
    </div>
  );
}
