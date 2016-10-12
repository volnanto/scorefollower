# scorefollower

ScoreFollower V1.0

This program is developed for BBC R&D by Bairong Han in 2016.

The Scorefollower is formed by two parts - MATCH (Credit: Dr Simon Dixon, QMUL; Reference: ) & WebApp.

MATCH is a An on-line time warping (OLTW) algorithm which is able to perform incremental alignment of arbitrarily long sequences in real time, presented by Simon Dixon. MATCH is based on an efficient time warping algorithm which has time and space costs that are linear in the lengths of the performances. The audio data is represented by positive spectral difference vectors. Frames of audio input are converted to a frequency domain representation using a short time Fourier transform, and then mapped to a non-linear frequency scale (linear at low frequencies and logarithmic at high frequencies). The time derivative of this spectrum is then half-wave rectified and the resulting vector is employed in the dynamic time warping algorithm’s match cost function, using a Euclidean metric.

Reference:
S Dixon, An on-line time warping algorithm for tracking musical performances. Proceedings of
the International Joint Conference, Jan 2005.
S. Dixon and G.Widmer, “MATCH: A music alignment tool chest,” in 6th International
Conference on Music Information Retrieval, 2005.

In the MATCH Java code, ruuning on Eclipse JAVA, few lines of code are added within ScrollingMatrix.java in order to perform a WebSocket Client End-point.

WebApp is a comunication tool developed for transporting data to AVP. It is running on NetBeans 7.4 with Glassfish Server 2.0. In fact WebApp is the WebSocket Server End-point which receiving and formating the data from MATCH and sending to AVP Client End-point.

All the data in txt format are generated from OFF-LINE ALIGHMENT (between reference recording and synthesised track) using SonicVisualiser MATCH plug-in, further processed by MATLAB for indexing purpose. Score images are produced by MuseScore.
