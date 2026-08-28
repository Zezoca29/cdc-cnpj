import { useEffect, useState } from 'react';
import { fetchHealth, fetchStats } from './api/client';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { EventExplorer } from './components/EventExplorer';
import { OverviewScreen } from './components/OverviewScreen';
import { EntityScreen } from './components/EntityScreen';
import { BenchmarkScreen } from './components/BenchmarkScreen';
import { QualityScreen } from './components/QualityScreen';
import type { DashboardStats, Screen } from './types/event';
import './styles/theme.css';

export default function App() {
  const [screen, setScreen] = useState<Screen>('events');
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [apiOnline, setApiOnline] = useState(false);

  useEffect(() => {
    async function bootstrap() {
      const online = await fetchHealth();
      setApiOnline(online);
      if (online) {
        try {
          const s = await fetchStats();
          setStats(s);
        } catch {
          setStats(null);
        }
      }
    }
    void bootstrap();
  }, []);

  return (
    <div className="app-shell">
      <Sidebar
        screen={screen}
        onNavigate={setScreen}
        totalEvents={stats?.totalEvents}
        competenciaDe={stats?.competenciaDe}
        competenciaPara={stats?.competenciaPara}
      />
      <main style={{ flex: 1, minWidth: 0, paddingBottom: 64 }}>
        <Header screen={screen} competenciaDe={stats?.competenciaDe} competenciaPara={stats?.competenciaPara} />
        {screen === 'overview' && <OverviewScreen stats={stats} apiOnline={apiOnline} />}
        {screen === 'events' && <EventExplorer stats={stats} />}
        {screen === 'entity' && <EntityScreen />}
        {screen === 'bench' && <BenchmarkScreen />}
        {screen === 'quality' && <QualityScreen stats={stats} />}
      </main>
    </div>
  );
}
