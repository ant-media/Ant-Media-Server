import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';
import Link from '@docusaurus/Link';


const FeatureList = [
  {
    title: 'How to Create Kubernetes Cluster on DigitalOcean',
    Svg: require('@site/static/img/undraw_docusaurus_mountain.svg').default,
    link: 'https://antmedia.io/how-to-create-kubernetes-cluster-on-digitalocean/',
    description: 'Ant Media Server provides a highly scalable solution on Kubernetes by the provided configuration files.'
  },
  {
    title: 'Installing AMS on Linux Server',
    Svg: require('@site/static/img/undraw_docusaurus_tree.svg').default,
    link: '/docs/guides/installing-on-linux/Installing-AMS-on-Linux/',
    description: 'Ant Media can be installed on Linux, particularly Ubuntu and CentOS distributions.',
  },
  {
    title: 'Simulcasting to social media channels',
    Svg: require('@site/static/img/undraw_docusaurus_tree.svg').default,
    link: '/docs/guides/publish-live-stream/Simulcasting/',
    description: 'This guide describes how to live stream to social media and other third party RTMP end points using Ant Media Server.',
  },
  {
    title: 'Ant Media Server REST API',
    Svg: require('@site/static/img/undraw_docusaurus_tree.svg').default,
    link: '/docs/developer-sdk-and-api/rest-api-guide/',
    description: 'When designing the Ant Media Server, we made sure everything is accessible through REST API.',
  }
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

function Card ({Svg, title, description, link}){
  return (
    <div className="card rounded col col-md-3 m-2">
      <Svg className={styles.featureSvg} role="img" />
      <h3 className="card-title">{title}</h3>
      <div className="card-body m-0 p-0">
        <p className="card-text">{description}</p>
      </div>
      <div className="mt-4 mb-2">
        <Link to={link} className="">Read the guide</Link>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row d-md-flex justify-content-center mb-4">
          
          
          <div className="card ms-2 me-2 rounded col col-md-3 m-2">
            <div className="card-body">
            <Link to="/docs/category/publish-live-stream/" className="d-inline">
              <h2 className="card-title">Publishing Live Streams</h2>
            </Link>
              <p className="card-text">Ant Media Server can ingest/accept WebRTC and RTMP streams. It can also re-stream RTMP, HLS and RTSP streams by pulling them from another stream source (e.g from a restreaming platform).</p>
            </div>
          </div>

          
          <div className="card ms-2 me-2 rounded col col-md-3 m-2">
            <div className="card-body">
            <Link to="/docs/category/playing-live-streams/" className="d-inline">
              <h2 className="card-title">Playing Live Streams</h2>
            </Link>
              <p className="card-text">WebRTC playback is only available in Enterprise Edition (EE). Before playing a stream with WebRTC, make sure that stream is broadcasting on the server.</p>
            </div>
          </div>

        </div>
        <div className="row">
          <h2>Popular Guides</h2>
          {FeatureList.map((props, idx) => (
            <Card key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
