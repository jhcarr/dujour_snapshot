-- Creating schema -- 

DROP TABLE releases, checkins, params;

CREATE TABLE releases (
    product text,
    version text,
    release_date timestamp,
    link text,
    message text,
    PRIMARY KEY (product, version)
);

CREATE TABLE checkins (
    checkin_id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP,
    product text,
    version text,
    FOREIGN KEY (product, version) REFERENCES releases,
    ip text
);

CREATE TABLE params (
    param text,
    value text,
    checkin_id INTEGER REFERENCES checkins,
    PRIMARY KEY (checkin_id)
);

CREATE INDEX checkins_timestamp
    ON checkins USING btree (timestamp);

CREATE INDEX checkins_ip
    ON checkins USING btree (ip);

-- Insert test fixtures -- 

DELETE FROM releases;

INSERT INTO releases (product, version, link, message)
VALUES ('pe-master', '3.0.0', 'http://links.puppetlabs.com/enterpriseupgrade', 'Version 3.0.0 of Puppet Enterprise is available'), ('pe-agent', '3.0.0', 'http://links.puppetlabs.com/enterpriseupgrade', 'Version 3.0.0 available for this Puppet Enterprise agent');

INSERT INTO releases (product, version, link)
VALUES ('puppetdb', '1.0.1', 'https://github.com/puppetlabs/puppetdb/blob/1.0.2/CHANGELOG.md');

SELECT * FROM releases;
