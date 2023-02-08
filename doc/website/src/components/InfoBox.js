import React from 'react';

export default function InfoBox({children}){
    return (
        <section className="infoBox">
            <div className="infobox-title">Note:</div>
            <div className="infobox-content">{children}</div>
        </section>
    )
}