CREATE TABLE dog (
                            id integer NOT NULL,
                            name text NOT NULL,
                            description text NOT NULL,
                            dob date NOT NULL,
                            owner text,
                            gender character(1) DEFAULT 'f'::bpchar NOT NULL,
                            image text NOT NULL
);

CREATE SEQUENCE dog_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE dog_id_seq OWNED BY dog.id;

ALTER TABLE ONLY dog ALTER COLUMN id SET DEFAULT nextval('dog_id_seq'::regclass);

CREATE TABLE authorities (
                                    username text NOT NULL,
                                    authority text NOT NULL
);

CREATE TABLE users (
                              username text NOT NULL,
                              password text NOT NULL,
                              enabled boolean NOT NULL
);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);


--
-- Name: ix_auth_username; Type: INDEX; Schema: public; Owner: myuser
--

CREATE UNIQUE INDEX ix_auth_username ON authorities USING btree (username, authority);


--
-- Name: authorities fk_authorities_users; Type: FK CONSTRAINT; Schema: public; Owner: myuser
--

ALTER TABLE ONLY authorities
    ADD CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username);

