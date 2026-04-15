import type { ReactNode } from 'react';

interface EditorLayoutProps {
  header: ReactNode;
  nodePanel: ReactNode;
  canvas: ReactNode;
  configPanel: ReactNode;
  debugDrawer: ReactNode;
  showConfigPanel: boolean;
}

export default function EditorLayout({
  header,
  nodePanel,
  canvas,
  configPanel,
  debugDrawer,
  showConfigPanel,
}: EditorLayoutProps) {
  return (
    <div style={{ width: '100vw', height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {header}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <div
          style={{
            width: 220,
            background: '#fff',
            borderRight: '1px solid #e5e7eb',
            overflowY: 'auto',
            flexShrink: 0,
          }}
        >
          {nodePanel}
        </div>
        <div style={{ flex: 1, position: 'relative' }}>{canvas}</div>
        {showConfigPanel && (
          <div
            style={{
              width: 420,
              background: '#fff',
              borderLeft: '1px solid #e5e7eb',
              overflowY: 'auto',
              flexShrink: 0,
            }}
          >
            {configPanel}
          </div>
        )}
      </div>
      {debugDrawer}
    </div>
  );
}
