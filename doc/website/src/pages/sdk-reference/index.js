import React from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import { styles } from 'prism-react-renderer/themes/github';

export default function SdkReferences() {
  return (
    <Layout title="Hello" description="Hello React Page">
      <main>
      <div style={{
          paddingTop: '20px',
          justifyContent: 'center',
          alignItems: 'center',
          fontSize: '20px',
          width: '800px',
          margin: 'auto'
        }}>
          <header>
          <h1>SDK reference Guide</h1>
          </header>
        
          <ul>
            <li>
              <Link to="/sdk-reference/video-effect">
                Video Effect
              </Link>
            </li>
            <li>
              <Link to="/sdk-reference/webrtc-adaptor">
                WebRTC Adaptor
              </Link>
            </li>
          </ul>
        </div>
      </main>
    </Layout>
  );
}