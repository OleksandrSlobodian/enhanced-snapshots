const path = require('path');
const webpack = require('webpack');
const autoprefixer = require('autoprefixer');
const HtmlPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require("extract-text-webpack-plugin");
//const ngAnnotatePlugin = require('ng-annotate-webpack-plugin');
//const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
    //context: path.resolve(__dirname, 'WebContent'),
    entry: {
        vendor: Object.keys(require('./package.json').dependencies),
        esnap: './js/app.js'
    },
    devtool: 'inline-source-map',
    output: {
        path: path.resolve(__dirname, 'dist'),
        publicPath: "http://localhost:5000",
        filename: '[name].min.js'
    },
    node: {
        fs: "empty",
        net: "empty",
        tls: "empty"
    },

    watchPoll: true,
    devServer: {
        watchPoll: true,
        port: 5000,
        inline: true,
        watchOptions: {
            ignored: /node_modules/,
            aggregateTimeout: 1500,
            poll: 1000
        },
        proxy: {
            '/rest/': {
                target: 'http://localhost:8080',
                secure: false
            }
        }
    },
    resolve: {
        modulesDirectories: [ 'node_modules'],
        extensions: [ '', '.js', '.css', '.json' ]
    },

    module: {
        preLoaders: [
            { test: /\.json$/, loader: 'json'}
        ],
        loaders: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                loader: 'ng-annotate!babel-loader'
            },
            {
                test: /\.html$/,
                exclude: path.resolve(__dirname, 'index.html'),
                loader: 'ng-cache'
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract('style', 'css-loader'),
                //loader: "style-loader!css-loader",
                exclude: path.resolve(__dirname, 'index.css'),
            },
            {
                test: /\.(ttf|eot|woff|woff2|png|ico|jpg|jpeg|gif|svg)$/i,
                loaders: [
                    "url-loader?mimetype=image/png"
                ]
            }
            //{
            //    test: /\.png$/,
            //    loader: "url-loader?mimetype=image/png"
            //}
        ]
    },
    plugins: [
        new ExtractTextPlugin('[name].css'),
        new webpack.ProvidePlugin({
            '_': 'lodash',
            '$': 'jquery',
            'jQuery': 'jquery',
            'window.jQuery': 'jquery'
        }),

        //new CopyWebpackPlugin([
        //    {
        //        from: '../WebContent',
        //        to: 'C:/Program Files/apache-tomcat-8.5.8/webapps/ROOT'
        //    }
        //], {
        //    copyUnmodified: true
        //}),

        new HtmlPlugin({
            title: 'Sungard Availability Services | Enhanced Snapshots',
            filename: 'index.html',
            favicon: './favicon.ico',
            template: path.resolve(__dirname, 'index.html')
        })
    ],
    'postcss': [
        autoprefixer({
            browsers: [
                'last 2 versions',
                'Explorer >= 11'
            ],
            cascade: false
        })
    ]
};